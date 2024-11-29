package com.example.attendace.Screens

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val courseId: Int = 0,
    val courseName: String,
    val location: String
)


@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class Schedule(
    @PrimaryKey(autoGenerate = true) val scheduleId: Int = 0,
    val courseId: Int, // Foreign key referencing Course
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val startTime: String, // Time in 24-hour format, e.g., "10:00"
    val endTime: String // Time in 24-hour format, e.g., "11:00"
)

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["courseId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["scheduleId"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"]), Index(value = ["scheduleId"])]
)

// CLasses to store the data fetched from the database
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val attendanceId: Int = 0,
    val courseId: Int,
    val scheduleId: Int, // Foreign key referencing Schedule
    val date: String, // Date in "yyyy-MM-dd" format
    val wasPresent: Boolean // True if the user was present
)

data class CourseDetails(
    val courseName: String,
    val location: String
)

data class CourseName(
    val courseId: Int,
    val courseName: String
)

data class AttendanceSummary(
    val totalEntries: Int,
    val presentCount: Int
)


@Dao
interface CourseDao {
    @Insert
    suspend fun insertCourse(course: Course): Long

    @Query("SELECT * FROM courses")
    suspend fun getAllCourses(): List<Course>

    @Query("SELECT courseName, location FROM courses WHERE courseId = :courseId")
    suspend fun getCourseDetails(courseId: Int): CourseDetails

    @Query("SELECT courseId, courseName FROM courses")
    suspend fun getCourseName(): List<CourseName>

    // Delete a specific course (cascading deletion will handle related schedules and attendance records)
    @Delete
    suspend fun deleteCourse(course: Course)
}

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertSchedule(schedule: Schedule)

    @Query("SELECT * FROM schedules WHERE courseId = :courseId")
    suspend fun getSchedulesForCourse(courseId: Int): List<Schedule>

    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<Schedule>

    @Query("SELECT * FROM schedules WHERE dayOfWeek = :dayOfWeek AND :time BETWEEN startTime AND endTime")
    suspend fun getCurrentlyScheduledClasses(dayOfWeek: String, time: String): Schedule

    // Delete a specific schedule (cascading deletion will handle related attendance records)
    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE courseId = :courseId")
    suspend fun deleteSchedulesForCourse(courseId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    // Get the schedule for a specific day of week
    @Query("SELECT * FROM schedules WHERE dayOfWeek = :dayOfWeek")
    suspend fun getSchedulesForDay(dayOfWeek: String): List<Schedule>
}

@Dao
interface AttendanceDao {
    @Insert
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records WHERE scheduleId = :scheduleId")
    suspend fun getAllAttendanceForSchedule(scheduleId: Int): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE courseId = :courseId")
    suspend fun getAllAttendanceForCourse(courseId: Int?): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllRecords(): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records WHERE scheduleId = :scheduleId")
    suspend fun deleteAttendanceForSchedule(scheduleId: Int)
}