import re
from flask import Flask, request, jsonify
import cv2
import pytesseract
import numpy as np
import os
from datetime import datetime

AttendAce = Flask(__name__)

# Set Tesseracts executable path
pytesseract.pytesseract.tesseract_cmd = r'"C:\Program Files\Tesseract-OCR\tesseract.exe"'

@AttendAce.route('/extract_table', methods=['POST'])
def extract_table():
    # Check if an image file was uploaded
    if 'image' not in request.files:
        return jsonify({"error": "No image file uploaded"}), 400

    file = request.files['image']

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # detect edges in the image (helps in detecting the lines)
    edges_image = detect_edges(file)

    # line detection in the image
    lines = detect_lines(edges_image)

    # get the diagonal coordinates of each box
    boxes = get_boxes(lines, file)

    # extract the text from each box
    extracted_data = extract_text_from_boxes(file, boxes)

    # Match the courses name with its time and day
    timetable = match_courses_with_time_and_day(extracted_data, days_of_week)

    # Return the timetable as JSON
    return jsonify(timetable)


def detect_edges(image):
    # Convert the image to grayscale
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Detect edges using Canny edge detector
    edges = cv2.Canny(gray_image, threshold1=30, threshold2=100)

    return edges

def detect_lines(edges_image):
    # Use Hough Line Transform to detect lines
    # The first two parameters define the resolution of the accumulator (1 pixel resolution, 1 degree angle resolution)
    # The threshold is the minimum number of intersections to detect a line
    lines = cv2.HoughLinesP(edges_image, 1, np.pi / 180, threshold=50, minLineLength=100, maxLineGap=7)

    return lines

def get_boxes(lines, image):
    # Create a copy of the original image to draw the lines
    output_image = image.copy()

    # Initialize lists to hold vertical and horizontal lines
    vertical_lines = []
    horizontal_lines = []

    # Classify the lines as vertical or horizontal based on their angle
    for line in lines:
        x1, y1, x2, y2 = line[0]
        # Calculate the slope (difference in y-coordinates / difference in x-coordinates)
        slope = (y2 - y1) / (x2 - x1) if x2 != x1 else np.inf  # Handle vertical lines

        # Define a threshold for distinguishing vertical and horizontal lines
        if abs(slope) < 1:  # If the slope is near 0, it's a horizontal line
            horizontal_lines.append((x1, y1, x2, y2))
        else:  # If the slope is large, it's a vertical line
            vertical_lines.append((x1, y1, x2, y2))

    # Sort the lines to make sure they are ordered from top to bottom (horizontal) and left to right (vertical)
    horizontal_lines = sorted(horizontal_lines, key=lambda line: line[1])  # Sort by y-coordinate (top to bottom)
    vertical_lines = sorted(vertical_lines, key=lambda line: line[0])  # Sort by x-coordinate (left to right)

    # Now, let's find the intersections of these lines to form the boxes
    boxes = []
    for i in range(len(horizontal_lines) - 1):
        for j in range(len(vertical_lines) - 1):
            # Get the coordinates of the top-left and bottom-right corners of the bounding box
            top_left_x = vertical_lines[j][0]
            top_left_y = horizontal_lines[i][1]
            bottom_right_x = vertical_lines[j + 1][0]
            bottom_right_y = horizontal_lines[i + 1][1]

            # Create a rectangle representing the bounding box
            boxes.append((top_left_x, top_left_y, bottom_right_x, bottom_right_y))

    # Draw the detected boxes on the image
    image_with_boxes = image.copy()
    for box in boxes:
        top_left = (box[0], box[1])
        bottom_right = (box[2], box[3])
        cv2.rectangle(image_with_boxes, top_left, bottom_right, (0, 255, 0), 2)

    return boxes

# Define list of unwanted words (e.g., lunch, recess, etc.)
words_for_break = ["lunch", "recess", "break"]

# Function to detect and extract text from each box in the timetable
def extract_text_from_boxes(image, boxes):
    extracted_data = []

    for i, box in enumerate(boxes):
        top_left = (box[0], box[1])
        bottom_right = (box[2], box[3])

        # Crop the image for each box
        cropped_image = image[top_left[1]:bottom_right[1], top_left[0]:bottom_right[0]]

        if cropped_image.size == 0:
            print(f"Warning: Cropped image at box {i} is empty!")
            continue

        # Use Tesseract to extract text from the cropped image
        text = pytesseract.image_to_string(cropped_image, config="--psm 6")

        cleaned_text = text.replace("\n", " ").replace("  ", " ").strip()
        cleaned_text = cleaned_text.replace(" - ", "-")
        cleaned_text = cleaned_text.replace("- ", "-")
        cleaned_text = cleaned_text.replace(" -", "-")

        # Clean the text (strip extra spaces, newline characters, etc.)
        cleaned_text = cleaned_text.strip().lower()

        # Store the extracted text (ignoring unwanted text)
        if not any(keyword in cleaned_text for keyword in words_for_break):
            extracted_data.append(cleaned_text)

    return extracted_data

# Function to check if a string represents a time
def is_time_string(text):
    # Define time formats: "HH:MM", "HH:MM AM/PM", "HH:MM-HH:MM"
    # Time ranges should have two times with a dash in between (e.g., "09:00-10:00")
    time_pattern = r'(\d{1,2}[:/.-]?\d{2}(?:\s?[APap][Mm])?)|(\d{1,2}[:/.-]?\d{2}-\d{1,2}[:/.-]?\d{2})'

    # Match only valid time formats
    match = re.match(time_pattern, text)

    # If a match is found, check for more constraints to avoid course names
    if match:
        # Check if the text looks like a valid time range or a time with AM/PM
        if '-' in text:
            # If it contains a '-', check that it's in the format "HH:MM-HH:MM"
            if re.match(r'\d{1,2}[:/.-]?\d{2}-\d{1,2}[:/.-]?\d{2}', text):
                return True
        elif ':' in text:
            # If it contains a ':', check if it's a valid time like "HH:MM" or "HH:MM AM/PM"
            if re.match(r'\d{1,2}[:/.-]?\d{2}', text):
                return True
    return False

def match_courses_with_time_and_day(extracted_data, days_of_week):
    timetable = {}

    current_time = None  # Variable to keep track of the current time slot

    # Iterate over the extracted course data
    for i, course in enumerate(extracted_data):
        course = course.strip().lower()  # Clean up the course text (remove extra spaces, etc.)

        # Check if the current course is a time string
        if is_time_string(course):
            current_time = course  # Update current time slot
        elif current_time is not None and course != "":
            # Determine the day by checking the index in the days_of_week list
            day_index = i % len(days_of_week)
            day = days_of_week[day_index].capitalize()  # Capitalize the day

            # Split the time period into start and end time
            start_time, end_time = current_time.split('-')

            # Convert time to 24-hour format
            start_time_24hr = convert_to_24hr(start_time)
            end_time_24hr = convert_to_24hr(end_time)

            # Store the course with its day, start time, and end time
            if course not in timetable:
                timetable[course] = []

            timetable[course].append([day, start_time_24hr, end_time_24hr])

    return timetable


# Function to convert time to 24-hour format
def convert_to_24hr(time_str):
    # Check if the time includes AM/PM
    if 'am' in time_str or 'pm' in time_str:
        time_obj = datetime.strptime(time_str, '%I:%M %p')
        return time_obj.strftime('%H:%M')  # Return time in 24-hour format
    else:
        return time_str  # If it's already in 24-hour format, just return it

# Sample days and times (you can modify this based on your timetable format)
days_of_week = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]

if __name__ == '__main__':
    AttendAce.run(debug=True)
