package com.example.core.backup.discovery

import android.util.Log
import com.example.core.drive.DriveFileInfo

class BackupDiscoveryManager(
    private val repository: BackupDiscoveryRepository
) : BackupDiscoveryProvider {

    companion object {
        private const val TAG = "BackupDiscoveryManager"
    }

    override suspend fun discoverBackups(): CloudBackupList {
        Log.i(TAG, "Discovering available backups in Google Drive App Data folder...")
        
        try {
            if (!repository.isDriveAuthorized()) {
                Log.w(TAG, "Google Drive app access is not authorized. Returning empty backup list.")
                return CloudBackupList(emptyList())
            }

            val driveFiles = repository.listCloudBackupFiles()


            val discoveredBackups = driveFiles.mapNotNull { fileInfo ->
                parseDriveFileInfo(fileInfo)
            }.sortedByDescending { it.createdDate }

            Log.i(TAG, "Discovered and parsed ${discoveredBackups.size} valid cloud backups (sorted newest first)")
            return CloudBackupList(discoveredBackups)

        } catch (e: Exception) {

            return CloudBackupList(emptyList())
        }
    }

    private fun parseDriveFileInfo(fileInfo: DriveFileInfo): CloudBackupInfo? {
        // Typically, our backup files might have standard names, or we can parse any file with backup metadata.
        // Let's inspect its custom metadata map first.
        val meta = fileInfo.metadata

        val backupId = meta["backupId"] ?: ""
        val checksum = meta["checksum"] ?: ""
        val appVersion = meta["appVersion"] ?: "1.0"
        val databaseVersionStr = meta["databaseVersion"] ?: "1"
        val databaseVersion = databaseVersionStr.toIntOrNull() ?: 1
        val deviceName = meta["deviceName"] ?: "Unknown Device"
        
        // Use the metadata createdDate if available; otherwise fallback to Google Drive's modifiedTime
        val createdDateStr = meta["createdDate"]
        val createdDate = createdDateStr?.toLongOrNull() ?: fileInfo.modifiedTime

        val backupVersionStr = meta["backupVersion"] ?: "1"
        val backupVersion = backupVersionStr.toIntOrNull() ?: 1

        // Return a fully mapped CloudBackupInfo object
        return CloudBackupInfo(
            id = fileInfo.id,
            fileName = fileInfo.name,
            backupId = backupId.ifBlank { fileInfo.id },
            backupVersion = backupVersion,
            createdDate = createdDate,
            sizeBytes = fileInfo.sizeBytes,
            deviceName = deviceName,
            appVersion = appVersion,
            databaseVersion = databaseVersion,
            checksum = checksum,
            extraMetadata = meta
        )
    }
}
