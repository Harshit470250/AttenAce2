package com.example.attendace

import android.app.Application
import com.example.attendace.Screens.AppDatabase
import com.example.attendace.Screens.DatabaseProvider

class AttendanceApp : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize the database
        database = DatabaseProvider.getDatabase(this)
    }
}
