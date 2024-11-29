package com.example.attendace

import com.example.attendace.Screens.HomeScreen
import com.example.attendace.Screens.CourseList
import com.example.attendace.Screens.CourseAttendanceLog
import com.example.attendace.Screens.AttendanceHistory
import com.example.attendace.Screens.AddCourseScreen
import com.example.attendace.Screens.BackgroundTaskWorker
import com.example.attendace.Screens.PickLocationScreen
import com.example.attendace.ui.theme.AttendAceTheme
import android.os.Bundle
import android.os.Build
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit
import com.jakewharton.threetenabp.AndroidThreeTen
//import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializing Shared Preferences
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)

        // Constraints for WOrk Manager -> Device must be connected to Network
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        // Initializing Work Manager
        val workRequest = PeriodicWorkRequestBuilder<BackgroundTaskWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)

        // Initialize the Threeten library to extract time, Can be used for API above 21
        AndroidThreeTen.init(this)

        setContent {
            AttendAceTheme {
                // Permissions to run access the location of user for background Tasks
                // Foreground Permission to access location while the app is running
                // Background Permission while app is running in Background -> can only be granted when foreground permission is granted
                var hasForegroundPermission by remember { mutableStateOf(false) }
                var hasBackgroundPermission by remember { mutableStateOf(false) }

                // Launcher to ask permission for the background
                val backgroundLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasBackgroundPermission = isGranted
                    SharedPrefHelper.saveBoolean(sharedPreferences, "hasBackgroundPermission", isGranted)
                }

                // Launcher to ask permission for the foreground
                val foregroundLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasForegroundPermission = isGranted

                    if (isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                    SharedPrefHelper.saveBoolean(sharedPreferences, "hasForegroundPermission", hasForegroundPermission)
                }

                LaunchedEffect(Unit) {
                    if (!SharedPrefHelper.getBoolean(sharedPreferences, "hasForegroundPermission", false)) {
                        foregroundLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }

                    // Before API 29, Background Location Access does not need to be asked separately, so ask permission for API above 29
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !SharedPrefHelper.getBoolean(sharedPreferences, "hasBackgroundPermission", false)) {
                        backgroundLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }

                AppNavigation(sharedPreferences)
            }
        }
    }
}

@Composable
fun AppNavigation(sharedPreferences: SharedPreferences) {
    // Initialize NavController
    val navController = rememberNavController()

    // Redirect the user to select the class location when the app is first Launched
    val isFirstLaunch = SharedPrefHelper.getBoolean(sharedPreferences, "isFirstLaunch", true) // Default value of First Launch is True i.e. when it's accessed first time
    val startDestination = if(isFirstLaunch) "map" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") { HomeScreen(navController) }
        composable("courses") { CourseList(navController) }
        composable("map") { PickLocationScreen(navController, sharedPreferences) }
        composable(
            "courseAttendance/{courseId}",
            arguments = listOf(navArgument("courseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val courseId = backStackEntry.arguments?.getInt("courseId")
            CourseAttendanceLog(navController, courseId)
        }
        composable("attendanceHistory") { AttendanceHistory(navController) }
        composable("addCourse") { AddCourseScreen(navController) }
    }
}

object SharedPrefHelper {
    fun saveBoolean(sharedPreferences: SharedPreferences, key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(sharedPreferences: SharedPreferences, key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun saveFloat(sharedPreferences: SharedPreferences, key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    fun getFloat(sharedPreferences: SharedPreferences, key: String, defaultValue: Float): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }
}