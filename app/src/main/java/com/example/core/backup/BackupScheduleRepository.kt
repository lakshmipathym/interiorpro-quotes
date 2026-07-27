package com.example.core.backup

import android.content.Context
import android.content.SharedPreferences

interface BackupScheduleRepository {
    fun getCurrentSchedule(): String
    fun setCurrentSchedule(scheduleId: String)
    fun getLastScheduledTime(): Long
    fun setLastScheduledTime(timestamp: Long)
}

class BackupScheduleRepositoryImpl(context: Context) : BackupScheduleRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("backup_schedule_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_SCHEDULE = "current_schedule"
        private const val KEY_LAST_SCHEDULED_TIME = "last_scheduled_time"
    }

    override fun getCurrentSchedule(): String {
        return prefs.getString(KEY_CURRENT_SCHEDULE, "MANUAL") ?: "MANUAL"
    }

    override fun setCurrentSchedule(scheduleId: String) {
        prefs.edit().putString(KEY_CURRENT_SCHEDULE, scheduleId).apply()
    }

    override fun getLastScheduledTime(): Long {
        return prefs.getLong(KEY_LAST_SCHEDULED_TIME, 0L)
    }

    override fun setLastScheduledTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SCHEDULED_TIME, timestamp).apply()
    }
}
