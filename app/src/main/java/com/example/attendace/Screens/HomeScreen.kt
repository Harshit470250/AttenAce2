package com.example.attendace.Screens

import android.content.pm.PackageManager
import com.example.attendace.AttendanceApp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.material.TopAppBar
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.material3.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.platform.LocalContext


@Composable
fun HomeScreen(navController: NavController? = null) {
    // Initializing the drawer
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Initializing the Coroutine use to perform asynchronous task
    val scope = rememberCoroutineScope()

    // Initializing object to access the database
    val database = AttendanceApp.database
    val scheduleDao = database.scheduleDao()
    val courseDao = database.courseDao()

    // State for permission check
    val context = LocalContext.current
    var hasStoragePermission by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    // Check if we have storage permission
    LaunchedEffect(Unit) {
        hasStoragePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request Permission if it's not granted
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasStoragePermission = granted
        }
    )

    // Open File Picker when "Upload Timetable" is clicked
    val openFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            selectedFileUri = uri
            // You can handle the file URI here
        }
    )

    // State to hold upcoming class and today's schedule
    var upcomingClass by remember { mutableStateOf<Pair<Schedule, String>?>(null) }
    var todaySchedule by remember { mutableStateOf(listOf<Pair<Schedule, String>>()) }

    // today's date and current time
    val today = LocalDateTime.now()
    val currentTime = LocalTime.now()

    // Check if we're on the HomeScreen in the back stack
    val currentBackStackEntry = navController?.currentBackStackEntryAsState()
    val isOnHomeScreen = currentBackStackEntry?.value?.destination?.route == "home"

    // Close the drawer so that it does no remains open while navigating back to the home screen from the screens available in drawer
    LaunchedEffect(isOnHomeScreen) {
        if (isOnHomeScreen) {
            drawerState.close()
        }
    }


    LaunchedEffect(Unit) {
        scope.launch {
            // Fetching today's schedule and course details
            val schedulesForToday = scheduleDao.getSchedulesForDay(today.dayOfWeek.name)
            todaySchedule = schedulesForToday.map { schedule ->
                val courseDetails = courseDao.getCourseDetails(schedule.courseId)
                schedule to courseDetails.courseName
            }

            // Finding the upcoming class
            upcomingClass = todaySchedule
                .filter { (schedule, _) ->
                    LocalTime.parse(
                        schedule.startTime,
                        DateTimeFormatter.ofPattern("HH:mm")
                    ) > currentTime
                }
                .minByOrNull { (schedule, _) ->
                    LocalTime.parse(
                        schedule.startTime,
                        DateTimeFormatter.ofPattern("HH:mm")
                    )
                }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (drawerState.isOpen) {
                Surface(
                    color = Color.White,
                    modifier = Modifier.width(280.dp)
                ) {
                    DrawerContent(navController)
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AttendAce",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                // Check if there is any upcoming class or not if there is show it in a box
                if (upcomingClass != null) {
                    UpcomingClassSection(navController, scheduleWithCourse = upcomingClass!!)
                } else {
//                    Button(
//                        onClick = {
//                            // Check if permission is granted
//                            if (hasStoragePermission) {
//                                // Open the file picker
//                                openFilePicker.launch("application/*") // You can modify MIME type to filter the file types
//                            } else {
//                                // Request permission if not granted
//                                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
//                            }
//                        },
//                        modifier = Modifier.align(Alignment.CenterHorizontally)
//                    ) {
//                        Text("Upload Timetable")
//                    }
                    Text("No Schedules" , modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // list all the classes that are scheduled for today
                    TodayScheduleSection(navController, todaySchedule)
                }
            }
        }
    }
}

@Composable
fun DrawerContent(navController: NavController?) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Text(text = "Add Course",
            modifier = Modifier
                .clickable {
                    navController?.navigate("addCourse")
                }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Courses",
            modifier = Modifier
                .clickable {
                    navController?.navigate("courses")
                }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Attendance Summary",
            modifier = Modifier
                .clickable {
                    navController?.navigate("attendanceHistory")
                }
        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(text = "Settings",
//            modifier = Modifier
//                .clickable {
//                    navController?.navigate("settings")
//                }
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(text = "Logout")
    }
}

@Composable
fun UpcomingClassSection(navController: NavController?, scheduleWithCourse: Pair<Schedule, String>) {
    // Display's the upcoming class
    val (schedule, courseName) = scheduleWithCourse

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { navController?.navigate("courseAttendance/${schedule.courseId}") },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Upcoming Class", style = MaterialTheme.typography.titleMedium)
            Text(text = "Course: $courseName")
            Text(text = "Time: ${schedule.startTime} - ${schedule.endTime}")
        }
    }
}

@Composable
fun TodayScheduleSection(navController: NavController?, todaySchedule: List<Pair<Schedule, String>>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Today's Schedule",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(todaySchedule) { (schedule, courseName) ->
                ClassCard(schedule, courseName, navController)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ClassCard(schedule: Schedule, courseName: String, navController: NavController?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController?.navigate("courseAttendance/${schedule.courseId}")
                },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Course: $courseName", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Time: ${schedule.startTime} - ${schedule.endTime}")
        }
    }
}