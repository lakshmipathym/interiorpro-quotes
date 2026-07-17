package com.example.core.sync

import com.example.core.drive.GoogleDriveService
import com.example.core.backup.BackupManager
import com.example.core.backup.RestoreManager
import com.example.core.device.DeviceManager

/**
 * SyncCoordinator orchestrates collaboration between Google Drive services, backup engines, and device profiling tools.
 */
interface SyncCoordinator {
    /**
     * Executes the orchestration logic to secure dynamic, atomic database and workspace data transfer.
     */
    suspend fun coordinateSync(
        driveService: GoogleDriveService,
        backupManager: BackupManager,
        restoreManager: RestoreManager,
        deviceManager: DeviceManager,
        onStateChanged: (SyncState) -> Unit = {}
    ): SyncResult
}
