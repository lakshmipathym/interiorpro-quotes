package com.example.core.backup

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

interface BackupScheduler {
    fun registerSchedule(scheduleId: String): Boolean
    fun cancelSchedule(): Boolean
    fun updateSchedule(scheduleId: String): Boolean
    fun getCurrentScheduleId(): String
    fun getWorkInfoFlow(): Flow<List<WorkInfo>>
    fun triggerInstantBackup()
}

class BackupSchedulerImpl(
    private val context: Context,
    private val repository: BackupScheduleRepository
) : BackupScheduler {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "BackupScheduler"
        private const val UNIQUE_WORK_NAME = "com.example.backup.PERIODIC_WORK"
        private const val ONE_TIME_WORK_TAG = "com.example.backup.ONE_TIME_WORK"
    }

    override fun registerSchedule(scheduleId: String): Boolean {
        Log.i(TAG, "Registering schedule for scheduleId: $scheduleId")
        
        // Cancel existing unique periodic work
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        
        when (scheduleId.uppercase()) {
            "MANUAL" -> {
                repository.setCurrentSchedule("MANUAL")
                return true
            }
            "ON_SAVE" -> {
                // "After Quotation Save" is triggered dynamically on save.
                // We save it as the current policy, and when quotes are saved, 
                // triggerInstantBackup() can be called by the system.
                repository.setCurrentSchedule("ON_SAVE")
                return true
            }
            "DAILY" -> {
                val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                    .setConstraints(getConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
                repository.setCurrentSchedule("DAILY")
                repository.setLastScheduledTime(System.currentTimeMillis())
                return true
            }
            "WEEKLY" -> {
                val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
                    .setConstraints(getConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
                repository.setCurrentSchedule("WEEKLY")
                repository.setLastScheduledTime(System.currentTimeMillis())
                return true
            }
            "MONTHLY" -> {
                // Approximate monthly with 30 days as standard
                val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.DAYS)
                    .setConstraints(getConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                    .build()
                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
                repository.setCurrentSchedule("MONTHLY")
                repository.setLastScheduledTime(System.currentTimeMillis())
                return true
            }
            else -> {
                Log.w(TAG, "Unknown schedule ID: $scheduleId")
                return false
            }
        }
    }

    override fun cancelSchedule(): Boolean {
        Log.i(TAG, "Canceling active backup schedule")
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        repository.setCurrentSchedule("MANUAL")
        return true
    }

    override fun updateSchedule(scheduleId: String): Boolean {
        return registerSchedule(scheduleId)
    }

    override fun getCurrentScheduleId(): String {
        return repository.getCurrentSchedule()
    }

    override fun getWorkInfoFlow(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
    }

    override fun triggerInstantBackup() {
        Log.i(TAG, "Triggering instant background backup via WorkManager")
        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .addTag(ONE_TIME_WORK_TAG)
            .setConstraints(getConstraints())
            .build()
        workManager.enqueue(workRequest)
    }

    private fun getConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}
