package com.example.attendace.Screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
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
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.attendace.AttendanceApp
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream


@Composable
fun HomeScreen(navController: NavController? = null) {
    // Initializing the drawer
    val drawerState = rememberDrawerState(DrawerValue.Open)

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

            scope.launch {
                handleFile(selectedFileUri, context, courseDao, scheduleDao)
            }
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
                                if (drawerState.isClosed) {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                } else {
                                    scope.launch {
                                        drawerState.close()
                                    }
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
                    Button(
                        onClick = {
                            if (hasStoragePermission) {
                                openFilePicker.launch("image/*")
                            } else {
                                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Upload Timetable")
                    }
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

suspend fun handleFile(uri: Uri?, context : Context, courseDao : CourseDao, scheduleDao : ScheduleDao) {
    try {
        // Gaining access to the content provider
        val contentResolver = context.contentResolver

        // Open an input stream to the selected file
        if(uri != null) {
            val inputStream = contentResolver.openInputStream(uri)

            // Storing the file
            val file = File(context.filesDir, "picked_image.png")

            val outputStream = FileOutputStream(file)

            // Copy data from the input stream (the selected file) to the output stream (the local file)
            inputStream?.copyTo(outputStream)

            // Close the streams
            inputStream?.close()
            outputStream.close()

            uploadImageToServer(file, courseDao, scheduleDao)
        }

    } catch (e: Exception) {
        Log.e("Upload Timetable", "Image not picked")
    }
}