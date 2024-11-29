package com.example.attendace.Screens

import com.example.attendace.AttendanceApp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import android.app.TimePickerDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(navController: NavController) {
    // initializing object to access the database
    val database = AttendanceApp.database
    val courseDao = database.courseDao()
    val scheduleDao = database.scheduleDao()

    // Initializing the Coroutine use to perform asynchronous task
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    // Details of the course to be filled
    var courseName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    val schedules = remember { mutableStateListOf<Schedule>() }

    // Drop down menu for selecting the day of the week
    val daysOfWeek = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    var isDayDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Add Course", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input for Course Name
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Input for Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Divider for Schedule Section as Course name will be fixed bu there may be different schedules for the same course
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Schedule", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Divider(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Dropdown for Day of Week
                Box {
                    OutlinedTextField(
                        value = selectedDay,
                        onValueChange = { selectedDay = it },
                        label = { Text("Day of Week") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { isDayDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Dropdown")
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = isDayDropdownExpanded,
                        onDismissRequest = { isDayDropdownExpanded = false }
                    ) {
                        daysOfWeek.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = day
                                    isDayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time Picker for Start Time
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it},
                    label = { Text("Start Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val timePickerDialog = TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    startTime = String.format("%02d:%02d", hour, minute)
                                },
                                12,
                                0,
                                true
                            )
                            timePickerDialog.show()
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Open Time Picker")
                        }
                    }
                )

                // Time Picker for End Time
                OutlinedTextField(
                    value = endTime,
                    onValueChange = {},
                    label = { Text("End Time") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val timePickerDialog = TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    endTime = String.format("%02d:%02d", hour, minute)
                                },
                                12, 0,
                                false
                            )
                            timePickerDialog.show()
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Open Time Picker")
                        }
                    }
                )

                // Save Schedule Button
                Button(
                    onClick = {
                        if (selectedDay.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                            schedules.add(
                                Schedule(
                                    scheduleId = 0,
                                    courseId = 0, // Temporary, will be assigned later
                                    dayOfWeek = selectedDay,
                                    startTime = startTime,
                                    endTime = endTime
                                )
                            )
                            // Fields specific to the schedules are cleared
                            selectedDay = ""
                            startTime = ""
                            endTime = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Save Schedule")
                }

                // Display List of Schedules
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    schedules.forEachIndexed { index, schedule ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    onClick = { schedules.removeAt(index) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Cancel", color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Save Course Button
                Button(
                    onClick = {
                        if (courseName.isNotBlank() && schedules.isNotEmpty()) {
                            coroutineScope.launch {
                                val courseId = courseDao.insertCourse(
                                    Course(courseName = courseName, location = location)
                                )
                                schedules.forEach { schedule ->
                                    scheduleDao.insertSchedule(
                                        schedule.copy(courseId = courseId.toInt())
                                    )
                                }
                                // Navigate back to the home screen after saving
                                navController.popBackStack()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Save Course")
                }
            }
        }
    )
}