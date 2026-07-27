package com.example.core.backup.discovery

import com.example.core.drive.DriveFileInfo
import com.example.core.drive.GoogleDriveService

class BackupDiscoveryRepositoryImpl(
    private val driveService: GoogleDriveService
) : BackupDiscoveryRepository {

    override suspend fun isDriveAuthorized(): Boolean {
        return driveService.isAuthorized()
    }

    override suspend fun listCloudBackupFiles(): List<DriveFileInfo> {
        return driveService.listAppDataFiles()
    }
}
