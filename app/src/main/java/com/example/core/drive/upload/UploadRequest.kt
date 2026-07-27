package com.example.core.drive.upload

import com.example.core.backup.pkg.BackupPackage

/**
 * UploadRequest models a request to upload a BackupPackage to Google Drive.
 */
data class UploadRequest(
    val backupPackage: BackupPackage,
    val passwordUsed: String,
    val customFileName: String? = null
)
