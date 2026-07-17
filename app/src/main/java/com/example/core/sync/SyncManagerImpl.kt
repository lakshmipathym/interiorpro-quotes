package com.example.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.core.drive.GoogleDriveService
import com.example.core.backup.BackupManager
import com.example.core.backup.RestoreManager
import com.example.core.backup.RestoreResult
import com.example.core.backup.BackupMetadata
import com.example.core.device.DeviceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class SyncManagerImpl(
    private val context: Context,
    private val driveService: GoogleDriveService,
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
    private val deviceManager: DeviceManager,
    private val syncCoordinator: SyncCoordinator,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : SyncManager {

    companion object {
        private const val TAG = "SyncManagerImpl"
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_POLICY = "auto_backup_policy"
        private const val KEY_LAST_SYNC = "last_sync_time_stamp"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    private val scope = CoroutineScope(dispatcher)

    init {
        // Register connection listener for automatic resume
        registerNetworkListener()
        // Initialize WorkManager backup policy schedule based on stored preferences
        schedulePolicyBackup(getAutoBackupPolicy())
    }

    override suspend fun triggerSync(): SyncResult {
        // 1. Check Connectivity
        if (!isNetworkConnected()) {
            Log.i(TAG, "Network is offline. Queueing sync and waiting for internet...")
            _syncState.value = SyncState.WaitingForInternet
            prefs.edit().putBoolean("sync_pending", true).apply()
            return SyncResult.Failure("Network is offline. Sync has been queued and will automatically resume once internet returns.")
        }

        // 2. Conflict Detection (Step 8)
        try {
            val cloudFiles = driveService.listAppDataFiles()
            val latestCloud = cloudFiles.maxByOrNull { it.modifiedTime }
            if (latestCloud != null) {
                val cloudDeviceId = latestCloud.metadata["deviceId"] ?: ""
                val cloudTimestamp = latestCloud.metadata["timestamp"]?.toLongOrNull() ?: 0L
                val localLastSync = prefs.getLong(KEY_LAST_SYNC, 0L)

                // If cloud is newer AND comes from a different device, detect conflict if local database also has new changes
                if (cloudDeviceId.isNotEmpty() && cloudDeviceId != deviceManager.getDeviceId() && cloudTimestamp > localLastSync) {
                    // Check if local DB has been modified since last sync
                    // Since we don't track every edit time precisely, we assume a conflict exists to avoid data loss
                    Log.w(TAG, "Sync Conflict detected: Cloud backup is newer and belongs to device: ${latestCloud.metadata["deviceName"]}")
                    _syncState.value = SyncState.Conflict
                    return SyncResult.Failure("Sync conflict detected: Cloud backup is newer and belongs to another device. Please resolve conflict manually.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Conflict check failed: ${e.message}")
        }

        return executeSyncInternal()
    }

    private suspend fun executeSyncInternal(): SyncResult {
        return try {
            val result = syncCoordinator.coordinateSync(
                driveService = driveService,
                backupManager = backupManager,
                restoreManager = restoreManager,
                deviceManager = deviceManager,
                onStateChanged = { newState ->
                    _syncState.value = newState
                    if (newState is SyncState.Success) {
                        val currentTime = System.currentTimeMillis()
                        prefs.edit().putLong(KEY_LAST_SYNC, currentTime).apply()
                        deviceManager.updateLastSyncTime(currentTime)
                    }
                }
            )
            prefs.edit().putBoolean("sync_pending", false).apply()
            result
        } catch (e: Exception) {
            val failedState = SyncState.Failed("Sync failed: ${e.message}", e)
            _syncState.value = failedState
            SyncResult.Failure("Sync execution error: ${e.message}", e)
        }
    }

    override suspend fun resolveConflicts(preferCloud: Boolean): SyncResult {
        _syncState.value = SyncState.Syncing
        return if (preferCloud) {
            // Force download and restore latest cloud backup
            try {
                val cloudFiles = driveService.listAppDataFiles()
                val latestCloud = cloudFiles.maxByOrNull { it.modifiedTime }
                if (latestCloud == null) {
                    _syncState.value = SyncState.Failed("No cloud backup located to restore.")
                    return SyncResult.Failure("No cloud backup found.")
                }
                val cacheDir = File(context.cacheDir, "staging")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val downloadFile = File(cacheDir, "conflict_cloud_backup.bin")
                val downloadSuccess = driveService.downloadFromAppData(latestCloud.id, downloadFile)
                if (downloadSuccess) {
                    val restoreResult = restoreManager.safeRestore(downloadFile, "")
                    if (restoreResult is RestoreResult.Success) {
                        _syncState.value = SyncState.Success(Date())
                        downloadFile.delete()
                        SyncResult.Success(syncedItemsCount = 1)
                    } else {
                        _syncState.value = SyncState.Failed("Restoration of cloud backup failed.")
                        SyncResult.Failure("Restore failed.")
                    }
                } else {
                    _syncState.value = SyncState.Failed("Failed to download cloud backup.")
                    SyncResult.Failure("Download failed.")
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Failed("Conflict resolution failed: ${e.message}")
                SyncResult.Failure("Exception during resolution: ${e.message}")
            }
        } else {
            // Overwrite cloud by executing normal upload sync
            executeSyncInternal()
        }
    }

    override suspend fun clearSyncState() {
        _syncState.value = SyncState.Idle
    }

    override fun onQuotationSaved() {
        scope.launch {
            val policy = getAutoBackupPolicy()
            if (policy == "ON_SAVE") {
                Log.i(TAG, "Auto Backup triggered on quote save via WorkManager...")
                
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                    
                val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.core.backup.BackupWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    .build()
                    
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "on_save_backup",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
            }
        }
    }

    override suspend fun checkForNewerBackup(): BackupMetadata? {
        if (!driveService.isAuthorized()) return null
        return try {
            val cloudFiles = driveService.listAppDataFiles()
            val latestCloud = cloudFiles.maxByOrNull { it.modifiedTime } ?: return null

            val cloudTimestamp = latestCloud.metadata["timestamp"]?.toLongOrNull() ?: 0L
            val localLastSync = prefs.getLong(KEY_LAST_SYNC, 0L)

            if (cloudTimestamp > localLastSync) {
                val metadata = BackupMetadata(
                    version = latestCloud.metadata["version"]?.toIntOrNull() ?: 1,
                    timestamp = cloudTimestamp,
                    checksum = latestCloud.metadata["checksum"] ?: "",
                    databaseVersion = latestCloud.metadata["databaseVersion"]?.toIntOrNull() ?: 6,
                    appVersion = latestCloud.metadata["appVersion"] ?: "1.5",
                    deviceId = latestCloud.metadata["deviceId"] ?: "unknown",
                    deviceName = latestCloud.metadata["deviceName"] ?: "Cloud Device",
                    isEncrypted = true,
                    isCompressed = true,
                    recordCount = 0
                )
                _syncState.value = SyncState.RestoreReady(metadata)
                metadata
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Check for newer backup failed: ${e.message}")
            null
        }
    }

    override fun getAutoBackupPolicy(): String {
        return prefs.getString(KEY_POLICY, "MANUAL") ?: "MANUAL"
    }

    override fun setAutoBackupPolicy(policy: String) {
        prefs.edit().putString(KEY_POLICY, policy).apply()
        Log.i(TAG, "Auto backup policy changed to: $policy")
        schedulePolicyBackup(policy)
    }

    private fun schedulePolicyBackup(policy: String) {
        try {
            val workManager = androidx.work.WorkManager.getInstance(context)
            val workName = "periodic_policy_backup"
            
            if (policy == "MANUAL" || policy == "ON_SAVE") {
                workManager.cancelUniqueWork(workName)
                Log.i(TAG, "Cancelled periodic backup work for policy: $policy")
                return
            }
            
            val repeatIntervalDays = when (policy) {
                "DAILY" -> 1L
                "WEEKLY" -> 7L
                "MONTHLY" -> 30L
                else -> return
            }
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
                
            val periodicWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.core.backup.BackupWorker>(
                repeatIntervalDays, java.util.concurrent.TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
                
            workManager.enqueueUniquePeriodicWork(
                workName,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
            Log.i(TAG, "Scheduled periodic backup work for policy: $policy with interval: $repeatIntervalDays days")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic backup via WorkManager", e)
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkListener() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        try {
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val isPending = prefs.getBoolean("sync_pending", false)
                    if (isPending) {
                        Log.i(TAG, "Internet is back! Resuming pending queued sync automatically...")
                        scope.launch {
                            executeSyncInternal()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register default network callback: ${e.message}")
        }
    }
}
