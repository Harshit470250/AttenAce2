package com.example.attendace.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import com.example.attendace.AttendanceApp
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseAttendanceLog(navController: NavController, courseId: Int?) {

    var attendanceRecord by remember { mutableStateOf(listOf<AttendanceRecord>()) }
    var scheduleList by remember { mutableStateOf(listOf<Schedule>()) }
    val schedules = remember { mutableStateListOf<Schedule>() }

    val database = AttendanceApp.database
    val attendanceDao = database.attendanceDao()
    val scheduleDao = database.scheduleDao()

    val coroutineScope = rememberCoroutineScope()

    // Get the attendance record when the screen is opened
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            attendanceRecord = attendanceDao.getAllAttendanceForCourse(courseId)
            if (courseId != null)
                scheduleList = scheduleDao.getSchedulesForCourse(courseId)

            schedules.clear()
            schedules.addAll(scheduleList)
        }
    }

    // Filter the attended classes
    val classesAttended = attendanceRecord.filter { it.wasPresent }

    // Update the the schedule based on what user have have changed when back button is pressed
    BackHandler {
        coroutineScope.launch {
            Log.d("Navigation", "Screen disposed")
            if (courseId != null) {
                scheduleDao.deleteSchedulesForCourse(courseId)
                scheduleDao.insertSchedules(schedules)
            }
        }
        navController.popBackStack()
    }

    // Update the the schedule based on what user have have changed when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {

                if (courseId != null) {
                    scheduleDao.deleteSchedulesForCourse(courseId)
                    scheduleDao.insertSchedules(schedules)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                Log.d("MyTagFinalListToBeUpdated", schedules.toString())
                                Log.d("Navigation", "Screen disposed")
                                if (courseId != null) {
                                    scheduleDao.deleteSchedulesForCourse(courseId)
                                    scheduleDao.insertSchedules(schedules)
                                }
                            }
                            navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Course Attendance",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->
            if (courseId != null)
                CourseAttendanceContent(
                    padding = padding,
                    totalClasses = attendanceRecord.size,
                    classesAttended = classesAttended.size,
                    schedules = schedules,
                    courseId = courseId
                )
        }
    )
}

@Composable
fun DonutChart(classesAttended: Int, classesMissed: Int) {
    val total = classesAttended + classesMissed
    val attendedAngle = (classesAttended.toFloat() / total) * 360f
    val missedAngle = 360f - attendedAngle

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            // Draw missed (background) slice
            drawArc(
                color = Color(0xFFADD8E6),
                startAngle = 0f,
                sweepAngle = missedAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx())
            )

            // Draw attended foreground slice
            drawArc(
                color = Color.Blue,
                startAngle = -90f,
                sweepAngle = attendedAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx())
            )
        }
    }
}

@Composable
fun CourseAttendanceContent(
    padding: PaddingValues,
    totalClasses: Int,
    classesAttended: Int,
    schedules: MutableList<Schedule>,
    courseId: Int
) {
    val classesMissed = totalClasses - classesAttended
    val attendancePercentage: Int = if (totalClasses == 0) {
        0
    } else {
        (classesAttended * 100) / totalClasses
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AttendanceRow(label = "Total Classes Conducted:", value = totalClasses.toString())
        AttendanceRow(label = "Classes You Attended:", value = classesAttended.toString())
        AttendanceRow(label = "Classes You Missed:", value = classesMissed.toString())
        AttendanceRow(label = "Attendance Rate:", value = "${attendancePercentage}%")

        Spacer(modifier = Modifier.height(16.dp))

        // Centered Donut Chart
        if (totalClasses != 0)
            DonutChart(classesAttended = classesAttended, classesMissed = classesMissed)

        Spacer(modifier = Modifier.height(16.dp))

        ScheduleCard(schedules, courseId)
    }
}

@Composable
fun AttendanceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScheduleCard(schedules: MutableList<Schedule>, courseId: Int) {
    if (schedules.isEmpty()) {
        Text(
            text = "No schedules available",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.Gray,
            fontSize = 16.sp
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            schedules.forEachIndexed { index, schedule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Day: ${schedule.dayOfWeek}")
                            Text("Start Time: ${schedule.startTime}")
                            Text("End Time: ${schedule.endTime}")
                        }
                        Button(
                            onClick = {
                                schedules.removeAt(index)
                                Log.d("MyTagChangeInScheduleList", schedules.toString())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
