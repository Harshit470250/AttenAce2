package com.example.attendace.Screens

import com.example.attendace.AttendanceApp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.IsoFields


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistory(navController: NavController) {
    // Attendance history can be viewed for the week, month and entire course
    var selectedPeriod by remember { mutableStateOf("Week") }

    // Initializing object to access the database
    val database = AttendanceApp.database
    val attendanceDao = database.attendanceDao()
    val courseDao = database.courseDao()

    val coroutineScope = rememberCoroutineScope()

    // Fetching the data Attendance History from the Database
    var attendanceRecord by remember { mutableStateOf(listOf<AttendanceRecord>()) }
    var courseNameList by remember { mutableStateOf(listOf<CourseName>()) }

    val today = LocalDate.now()


    LaunchedEffect(Unit) {
        coroutineScope.launch {
            attendanceRecord = attendanceDao.getAllRecords()
        }
    }
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            courseNameList = courseDao.getCourseName()
        }
    }


    val weeklyRecord = weeklyAttendanceSummary(attendanceRecord, today)
    val monthlyRecord = monthlyAttendanceSummary(attendanceRecord, today)
    val overallRecord = overallAttendanceSummary(attendanceRecord)
    val courseName = courseNameList.associateBy({ it.courseId }, { it.courseName })

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
                        Text(
                            text = "Attendance History",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Top Row for buttons to select to view weekly, monthly or overall records
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PeriodButton(text = "Week", isSelected = selectedPeriod == "Week") {
                        selectedPeriod = "Week"
                    }
                    PeriodButton(text = "Month", isSelected = selectedPeriod == "Month") {
                        selectedPeriod = "Month"
                    }
                    PeriodButton(text = "Overall", isSelected = selectedPeriod == "Overall") {
                        selectedPeriod = "Overall"
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display the content based on the selected period
                when (selectedPeriod) {
                    "Week" -> AttendanceListContent("Past Week", navController, courseName, weeklyRecord)
                    "Month" -> AttendanceListContent("Past Month", navController, courseName, monthlyRecord)
                    "Overall" -> AttendanceListContent("Overall Attendance", navController, courseName, overallRecord)
                }
            }
        }
    )
}

@Composable
fun PeriodButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AttendanceListContent(periodLabel: String,
                          navController: NavController?,
                          courseName: Map<Int, String>,
                          record : Map<Int, AttendanceSummary>
                          ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = periodLabel,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        val recordEntries = record.entries
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(record.size) { index ->
                val entry = recordEntries.elementAt(index)
                val courseId = entry.key
                val attendanceSummary = entry.value
                val courseTitle = courseName[courseId] ?: "Unknown Course"
                AttendanceCard(course = courseTitle to attendanceSummary, navController = navController, courseId = courseId)
            }
        }
    }
}

@Composable
fun AttendanceCard(course: Pair<String, AttendanceSummary>, navController: NavController?, courseId : Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { navController?.navigate("courseAttendance/$courseId") }
        ,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = "${course.first} - Attended: ${course.second.presentCount} / ${course.second.totalEntries}",
                fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

fun overallAttendanceSummary(
    records: List<AttendanceRecord>,
): Map<Int, AttendanceSummary> {
    return records.groupBy { it.courseId } // Group records by courseId
        .mapValues { (_, courseRecords) ->
            val totalEntries = courseRecords.size
            val presentCount = courseRecords.count { it.wasPresent }
            AttendanceSummary(totalEntries, presentCount)
        }
}

fun weeklyAttendanceSummary(
    records: List<AttendanceRecord>,
    date: LocalDate
): Map<Int, AttendanceSummary> {
    // Extract the year and week of the given date
    val targetYear = date.get(IsoFields.WEEK_BASED_YEAR)
    val targetWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    // Filter records to include only those in the same week as the given date
    val filteredRecords = records.filter {
        val recordDate = LocalDate.parse(it.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val recordYear = recordDate.get(IsoFields.WEEK_BASED_YEAR)
        val recordWeek = recordDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        recordYear == targetYear && recordWeek == targetWeek
    }

    // Group by courseId and calculate summary
    return filteredRecords.groupBy { it.courseId }
        .mapValues { (_, courseRecords) ->
            val totalEntries = courseRecords.size
            val presentCount = courseRecords.count { it.wasPresent }
            AttendanceSummary(totalEntries, presentCount)
        }
}

fun monthlyAttendanceSummary(
    records: List<AttendanceRecord>,
    date: LocalDate
): Map<Int, AttendanceSummary> {
    // Extract the target year and month
    val targetYear = date.year
    val targetMonth = date.monthValue

    // Filter records to include only those in the same month and year as the given date
    val filteredRecords = records.filter {
        val recordDate = LocalDate.parse(it.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        recordDate.year == targetYear && recordDate.monthValue == targetMonth
    }

    // Group by courseId and calculate summary
    return filteredRecords.groupBy { it.courseId }
        .mapValues { (_, courseRecords) ->
            val totalEntries = courseRecords.size
            val presentCount = courseRecords.count { it.wasPresent }
            AttendanceSummary(totalEntries, presentCount)
        }
}