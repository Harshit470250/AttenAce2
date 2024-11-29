//package com.example.attendace.Screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
////import androidx.compose.material.icons.filled.DarkMode
////import androidx.compose.material.icons.filled.LightMode
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import android.content.Context
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import androidx.compose.runtime.collectAsState
//import kotlinx.coroutines.launch
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SettingsPage(navController: NavController) {
//        var notificationsEnabled by remember { mutableStateOf(false) }
//    var minAttendance by remember { mutableStateOf("75") }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primary
//                )
//            )
//        }
//    ) {
//        padding -> Column(
//
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            // Notifications Toggle
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text("Notifications", fontSize = 16.sp)
//                Spacer(modifier = Modifier.weight(1f))
//                Switch(
//                    checked = notificationsEnabled,
//                    onCheckedChange = { notificationsEnabled = it }
//                )
//            }
//
//            // Minimum Attendance Requirement
//            Column {
//                Text("Minimum Attendance Requirement (%)", fontSize = 16.sp)
//                Spacer(modifier = Modifier.height(8.dp))
//                BasicTextField(
//                    value = minAttendance,
//                    onValueChange = { minAttendance = it },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(Color.LightGray, MaterialTheme.shapes.small)
//                        .padding(8.dp),
//                    singleLine = true
//                )
//            }
//
//            // Reset Attendance Data Button
//            Button(
//                onClick = { /* Implement reset attendance data logic */ },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//            ) {
//                Text("Reset Attendance Data", color = Color.White)
//            }
//
//            // Data Backup Button
//            Button(
//                onClick = { /* Implement data backup logic */ },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Data Backup")
//            }
//
//            // Clear Data Button
//            Button(
//                onClick = { /* Implement clear data logic */ },
//                modifier = Modifier.fillMaxWidth(),
//                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//            ) {
//                Text("Clear Data", color = Color.White)
//            }
//        }
//    }
//}
//
//
//
//private val Context.dataStore by preferencesDataStore("settings")
//
//class ThemePreferenceManager(private val context: Context) {
//
//    companion object {
//        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_enabled")
//    }
//
//    val isDarkTheme: Flow<Boolean> = context.dataStore.data
//        .map { preferences ->
//            preferences[DARK_THEME_KEY] ?: false  // Default to false (light theme)
//        }
//
//    suspend fun saveThemePreference(isDarkTheme: Boolean) {
//        context.dataStore.edit { preferences ->
//            preferences[DARK_THEME_KEY] = isDarkTheme
//        }
//    }
//}