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
    val courseId: Int,
    val dayOfWeek: String, // "MONDAY", "TUESDAY" like format
    val startTime: String, // 24 hour time format "13:00"
    val endTime: String // 24 hour time format "14:00"
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
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val attendanceId: Int = 0,
    val courseId: Int,
    val scheduleId: Int,
    val date: String, // Date in "yyyy-MM-dd" format
    val wasPresent: Boolean
)

// CLasses to store the data fetched from the database
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

data class CourseScheduleMap(
    val courseName: String,
    val schedules: List<ScheduleEntry>
)

data class ScheduleEntry(
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
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

    @Query("DELETE FROM schedules WHERE courseId = :courseId")
    suspend fun deleteSchedulesForCourse(courseId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Query("SELECT * FROM schedules WHERE dayOfWeek = :dayOfWeek")
    suspend fun getSchedulesForDay(dayOfWeek: String): List<Schedule>

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)
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

    @Query("SELECT COUNT(1) FROM attendance_records WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun getIfAttendedClass(scheduleId: Int, date: String) : Int

    @Query("DELETE FROM attendance_records WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun deletePreviousUnmarkedAttendance(scheduleId: Int, date: String)
}