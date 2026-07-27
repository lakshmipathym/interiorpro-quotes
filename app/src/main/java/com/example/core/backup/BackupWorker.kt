package com.example.core.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "BackupWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "BackupWorker skeleton execution: Triggered background backup schedule placeholder")
        // No backup execution, Google Drive upload, encryption, or restore logic yet.
        // Return Success as requested.
        return Result.success()
    }
}
