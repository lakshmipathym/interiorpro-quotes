package com.example.core.sync

import android.util.Log
import com.example.core.drive.GoogleDriveService
import com.example.core.backup.BackupManager
import com.example.core.backup.BackupResult
import com.example.core.backup.RestoreManager
import com.example.core.backup.RestoreResult
import com.example.core.device.DeviceManager
import java.io.File
import java.util.Date

class SyncCoordinatorImpl : SyncCoordinator {

    companion object {
        private const val TAG = "SyncCoordinatorImpl"
        private const val BACKUP_PASSWORD = "" // Using default secure key pairing
    }

    override suspend fun coordinateSync(
        driveService: GoogleDriveService,
        backupManager: BackupManager,
        restoreManager: RestoreManager,
        deviceManager: DeviceManager,
        onStateChanged: (SyncState) -> Unit
    ): SyncResult {
        
        // 1. Verify Authentication Status
        if (!driveService.isAuthorized()) {
            Log.w(TAG, "Sync aborted: Google Drive is not connected")
            onStateChanged(SyncState.NotConnected)
            return SyncResult.Failure("Google Drive is not authenticated or connected")
        }

        onStateChanged(SyncState.Connected)
        onStateChanged(SyncState.Syncing)

        // Setup temporary workspace files
        val cacheDir = File(System.getProperty("java.io.tmpdir") ?: "/tmp").apply {
            if (!exists()) mkdirs()
        }
        val uploadStagingFile = File(cacheDir, "staging_upload_backup.bin")
        val downloadStagingFile = File(cacheDir, "staging_download_backup.bin")

        try {
            // ==========================================
            // STEP 3: Backup Upload Pipeline
            // ==========================================
            Log.i(TAG, "Executing Backup Upload Pipeline (Step 3)...")
            
            // 3.1 Create, Compress, AES Encrypt, Checksum, and generate local metadata
            val backupResult = backupManager.createBackup(
                destinationFile = uploadStagingFile,
                password = BACKUP_PASSWORD,
                encrypt = true,
                compress = true
            )

            if (backupResult is BackupResult.Failure) {
                onStateChanged(SyncState.Failed("Backup creation failed: ${backupResult.reason}"))
                return SyncResult.Failure("Backup upload pipeline failed at backup stage: ${backupResult.reason}")
            }

            val successBackup = backupResult as BackupResult.Success
            val localMetadata = successBackup.metadata

            // 3.2 Upload Metadata & Data to Google Drive secure appDataFolder
            onStateChanged(SyncState.Uploading)
            Log.d(TAG, "Uploading archive file to Google Drive App Data folder...")
            
            val cloudProperties = mapOf(
                "checksum" to localMetadata.checksum,
                "timestamp" to localMetadata.timestamp.toString(),
                "appVersion" to localMetadata.appVersion,
                "databaseVersion" to localMetadata.databaseVersion.toString(),
                "deviceId" to localMetadata.deviceId,
                "deviceName" to localMetadata.deviceName
            )

            val uploadedFileId = driveService.uploadToAppData(
                file = uploadStagingFile,
                mimeType = "application/octet-stream",
                metadata = cloudProperties
            )

            if (uploadedFileId.isEmpty()) {
                onStateChanged(SyncState.Failed("Upload payload failed to register with Google Drive REST api"))
                return SyncResult.Failure("Google Drive upload failed")
            }

            // 3.3 Verify Upload
            Log.d(TAG, "Verifying uploaded file in Google Drive App Data folder...")
            val cloudFiles = driveService.listAppDataFiles()
            val matchedFile = cloudFiles.find { it.id == uploadedFileId }
            if (matchedFile == null) {
                onStateChanged(SyncState.Failed("Upload verification failed: file not found in App Data Folder"))
                return SyncResult.Failure("Google Drive upload verification failed: uploaded file not found in active space")
            }

            Log.i(TAG, "Backup Upload Pipeline completed and verified successfully. Cloud File ID: $uploadedFileId")

            // ==========================================
            // STEP 4: Download & Validation Pipeline
            // ==========================================
            Log.i(TAG, "Executing Download & Validation Pipeline (Step 4)...")
            onStateChanged(SyncState.Downloading)

            // 4.1 Locate Latest Backup
            val latestCloudFile = cloudFiles.maxByOrNull { it.modifiedTime }
            if (latestCloudFile == null) {
                onStateChanged(SyncState.Failed("Download pipeline failed: No cloud backup located"))
                return SyncResult.Failure("No backups located on Google Drive to test download pipeline")
            }

            // 4.2 Download payload to temporary staging space
            Log.d(TAG, "Downloading latest archive payload (${latestCloudFile.id}) to staging file...")
            val downloadSuccess = driveService.downloadFromAppData(latestCloudFile.id, downloadStagingFile)
            if (!downloadSuccess) {
                onStateChanged(SyncState.Failed("Download failed from Google Drive"))
                return SyncResult.Failure("Google Drive file download failed during verification")
            }

            // 4.3 Verify SHA-256 Checksum, Decrypt, Temporary Restore, and Validate schema
            Log.d(TAG, "Validating and decrypting downloaded payload...")
            val integrityOk = restoreManager.verifyBackupIntegrity(downloadStagingFile, BACKUP_PASSWORD)
            if (!integrityOk) {
                onStateChanged(SyncState.Failed("Downloaded backup failed cryptographic integrity verification"))
                return SyncResult.Failure("Downloaded backup integrity verification failed")
            }

            val restoreResult = restoreManager.safeRestore(downloadStagingFile, BACKUP_PASSWORD)
            if (restoreResult is RestoreResult.InvalidBackup) {
                onStateChanged(SyncState.Failed("Temporary restore validation failed: ${restoreResult.reason}"))
                return SyncResult.Failure("Download pipeline failed at staging restore validation: ${restoreResult.reason}")
            } else if (restoreResult is RestoreResult.Rollback) {
                onStateChanged(SyncState.Failed("Staging restore validation threw exception: ${restoreResult.reason}"))
                return SyncResult.Failure("Download pipeline threw during staging restore: ${restoreResult.reason}")
            }

            // All systems are GO and validated!
            Log.i(TAG, "Download and verification pipeline completed and validated successfully.")
            onStateChanged(SyncState.Success(Date()))
            return SyncResult.Success(syncedItemsCount = localMetadata.recordCount)

        } catch (e: Exception) {
            Log.e(TAG, "Fatal uncaught exception in Sync Coordinator orchestration", e)
            onStateChanged(SyncState.Failed("Sync coordinator fatal exception: ${e.message}", e))
            return SyncResult.Failure("Fatal sync coordinator exception: ${e.message}", e)
        } finally {
            // Clean up temporary local files
            if (uploadStagingFile.exists()) uploadStagingFile.delete()
            if (downloadStagingFile.exists()) downloadStagingFile.delete()
        }
    }
}
