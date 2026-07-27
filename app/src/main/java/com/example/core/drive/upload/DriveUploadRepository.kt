package com.example.core.drive.upload

import com.example.core.backup.pkg.BackupPackage
import java.io.File

interface DriveUploadRepository {
    fun isNetworkAvailable(): Boolean
    suspend fun isDriveAuthorized(): Boolean
    suspend fun uploadToAppData(
        backupPackage: BackupPackage,
        tempFile: File,
        metadata: Map<String, String>
    ): String
}
