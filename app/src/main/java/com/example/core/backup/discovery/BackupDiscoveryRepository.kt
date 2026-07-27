package com.example.core.backup.discovery

import com.example.core.drive.DriveFileInfo

interface BackupDiscoveryRepository {
    suspend fun isDriveAuthorized(): Boolean
    suspend fun listCloudBackupFiles(): List<DriveFileInfo>
}
