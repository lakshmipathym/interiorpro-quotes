package com.example.core.backup

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.core.device.DeviceManagerImpl
import com.example.core.drive.GoogleSignInManagerImpl
import com.example.core.drive.GoogleDriveServiceImpl
import com.example.core.security.EncryptionManagerImpl
import com.example.core.security.ChecksumManagerImpl
import com.example.core.security.IntegrityValidatorImpl
import java.io.File

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "BackupWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting WorkManager-managed background backup execution...")
        
        try {
            val context = applicationContext
            val database = AppDatabase.getDatabase(context)
            val repository = QuotesRepository(database)
            
            val signInManager = GoogleSignInManagerImpl(context)
            val driveService = GoogleDriveServiceImpl(context, signInManager)
            val encryptionManager = EncryptionManagerImpl()
            val checksumManager = ChecksumManagerImpl()
            val integrityValidator = IntegrityValidatorImpl(checksumManager)
            val restoreManager = RestoreManagerImpl(context, database, repository, encryptionManager, checksumManager, integrityValidator)
            val deviceManager = DeviceManagerImpl(context)
            val backupManager = BackupManagerImpl(database, repository, encryptionManager, checksumManager, deviceManager)
            
            // 1. Silent Sign-in Check
            if (!signInManager.isUserSignedIn.value) {
                Log.d(TAG, "Silent signing in...")
                val signedIn = signInManager.silentSignIn()
                if (!signedIn) {
                    Log.w(TAG, "Cannot run backup: User is not authenticated with Google Drive.")
                    return Result.failure()
                }
            }
            
            // 2. Perform Backup to a temporary file
            val tempBackupFile = File(context.cacheDir, "workmanager_backup_temp.bin")
            if (tempBackupFile.exists()) tempBackupFile.delete()
            
            val backupResult = backupManager.createBackup(
                destinationFile = tempBackupFile,
                password = "", // Default password
                encrypt = true,
                compress = true
            )
            
            if (backupResult is BackupResult.Failure) {
                Log.e(TAG, "Backup creation failed in background: ${backupResult.reason}")
                return Result.retry()
            }
            
            val successResult = backupResult as BackupResult.Success
            
            // 3. Upload to Google Drive App Data Space
            val mimeType = "application/octet-stream"
            val metadata = mapOf(
                "version" to successResult.metadata.version.toString(),
                "timestamp" to successResult.metadata.timestamp.toString(),
                "checksum" to successResult.metadata.checksum,
                "databaseVersion" to successResult.metadata.databaseVersion.toString(),
                "appVersion" to successResult.metadata.appVersion,
                "deviceId" to successResult.metadata.deviceId,
                "deviceName" to successResult.metadata.deviceName,
                "isEncrypted" to successResult.metadata.isEncrypted.toString(),
                "isCompressed" to successResult.metadata.isCompressed.toString()
            )
            
            val fileId = driveService.uploadToAppData(tempBackupFile, mimeType, metadata)
            
            // Clean up temporary file
            if (tempBackupFile.exists()) {
                tempBackupFile.delete()
            }
            
            if (fileId.isEmpty()) {
                Log.e(TAG, "Google Drive upload failed in background.")
                return Result.retry()
            }
            
            Log.i(TAG, "Background backup successfully completed and uploaded to Google Drive. FileId: $fileId")
            
            // Save last sync time in preferences
            val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val currentTime = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time_stamp", currentTime).apply()
            deviceManager.updateLastSyncTime(currentTime)
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in background BackupWorker: ${e.message}", e)
            return Result.retry()
        }
    }
}
