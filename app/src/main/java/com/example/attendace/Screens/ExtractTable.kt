package com.example.attendace.Screens

import okhttp3.MultipartBody
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Multipart
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import android.util.Log

interface ApiService {
    @Multipart
    @POST("/extract_table")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<List<CourseScheduleMap>>
}

object RetrofitInstance {
    private const val BASE_URL = "http://127.0.0.1:5000"

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

suspend fun uploadImageToServer(file: File, courseDao: CourseDao, scheduleDao: ScheduleDao) {
    // Prepare the image file for upload by making the multi-part body
    val requestFile = RequestBody.create("image/*".toMediaType(), file)
    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

    try {
        val response = RetrofitInstance.apiService.uploadImage(body)

        if (response.isSuccessful) {
            val courseScheduleMapList = response.body() ?: emptyList()

            // Handle the schedule data
            insertCoursesAndSchedules(courseScheduleMapList, courseDao, scheduleDao)
        }
    } catch (e: Exception) {
        Log.e("Image", "Network error occurred: ${e.message}")
    }
}

suspend fun insertCoursesAndSchedules(courseScheduleMapList: List<CourseScheduleMap>, courseDao: CourseDao, scheduleDao: ScheduleDao) {
    for (courseScheduleMap in courseScheduleMapList) {
        val course = Course(courseName = courseScheduleMap.courseName, location = "default location")
        val courseId = courseDao.insertCourse(course)

        for (scheduleEntry in courseScheduleMap.schedules) {
            val schedule = Schedule(
                courseId = courseId.toInt(),
                dayOfWeek = scheduleEntry.dayOfWeek,
                startTime = scheduleEntry.startTime,
                endTime = scheduleEntry.endTime
            )
            scheduleDao.insertSchedule(schedule)
        }
    }
}