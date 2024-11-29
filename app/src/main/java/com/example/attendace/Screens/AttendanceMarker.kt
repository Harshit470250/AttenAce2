package com.example.attendace.Screens

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.location.Location
import androidx.work.CoroutineWorker
import com.example.attendace.AttendanceApp
import com.example.attendace.SharedPrefHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class BackgroundTaskWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = coroutineScope {
        Log.d("BackgroundTaskWorker", "Running background task...")


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        val sharedPreferences = applicationContext.getSharedPreferences("app_preferences", MODE_PRIVATE)
        val isLocationPermission = SharedPrefHelper.getBoolean(sharedPreferences, "hasForegroundPermission", false) &&
                SharedPrefHelper.getBoolean(sharedPreferences, "hasBackgroundPermission", false)

        if (!isLocationPermission) {
            Log.e("BackgroundTaskWorker", "Location permissions are not granted.")
            return@coroutineScope Result.failure()
        }

        val classLatitude = sharedPreferences.getFloat("picked_latitude", 0f)
        val classLongitude = sharedPreferences.getFloat("picked_longitude", 0f)

        val database = AttendanceApp.database
        val scheduleDao = database.scheduleDao()
        val attendanceDao = database.attendanceDao()

        // getting current time and date
        val calendar = Calendar.getInstance()
        val dayOfWeek = getDayName()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todaysDate = dateFormat.format(calendar.time)

        // Find the class scheduled for current time and date
        val currentlyScheduledClasses = scheduleDao.getCurrentlyScheduledClasses(dayOfWeek, "$hour:$minute")

        // Fetch user location
        val userLocation = getUserLocation()
        if (userLocation != null) {
            val distanceBetweenUserAndClass =
                distanceBetweenUserAndClass(userLocation, classLatitude, classLongitude)


            val attendanceRecord = AttendanceRecord(
                attendanceId = 0,
                courseId = currentlyScheduledClasses.courseId,
                scheduleId = currentlyScheduledClasses.scheduleId,
                date = todaysDate,
                wasPresent = false
            )

            if (distanceBetweenUserAndClass <= 50) {
                attendanceDao.insertAttendanceRecord(attendanceRecord.copy(wasPresent = true))
            } else {
                attendanceDao.insertAttendanceRecord(attendanceRecord)
            }

        } else {
            Log.e("BackgroundTaskWorker", "User location could not be retrieved.")
        }

        Result.success()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getUserLocation(): Location? = suspendCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && location.accuracy <= 50) {
                continuation.resume(location)
            } else {
                fusedLocationClient.getCurrentLocation(
                    LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, null
                ).addOnSuccessListener { freshLocation ->
                    continuation.resume(freshLocation)
                }.addOnFailureListener { exception ->
                    Log.e("BackgroundTaskWorker", "Failed to retrieve location: ${exception.message}")
                    continuation.resume(null)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("BackgroundTaskWorker", "Failed to retrieve location: ${exception.message}")
            continuation.resume(null)
        }
    }

}


fun getDayName(): String {
    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val dayName = when (dayOfWeek) {
        Calendar.SUNDAY -> "SUNDAY"
        Calendar.MONDAY -> "MONDAY"
        Calendar.TUESDAY -> "TUESDAY"
        Calendar.WEDNESDAY -> "WEDNESDAY"
        Calendar.THURSDAY -> "THURSDAY"
        Calendar.FRIDAY -> "FRIDAY"
        Calendar.SATURDAY -> "SATURDAY"
        else -> "UNKNOWN"
    }

    return dayName
}

// Find the distance between user and class based on latitudes and longitudes
fun distanceBetweenUserAndClass(location: Location, lat2: Float, lon2: Float): Float {
    val startPoint = Location("startPoint").apply {
        latitude = location.latitude.toDouble()
        longitude = location.longitude.toDouble()
    }

    val endPoint = Location("endPoint").apply {
        latitude = lat2.toDouble()
        longitude = lon2.toDouble()
    }

    return startPoint.distanceTo(endPoint)
}


