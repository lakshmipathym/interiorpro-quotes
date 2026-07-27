package com.example.core.drive.upload

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.core.backup.pkg.BackupPackage
import com.example.core.drive.GoogleDriveService
import java.io.File

class DriveUploadRepositoryImpl(
    private val context: Context,
    private val driveService: GoogleDriveService
) : DriveUploadRepository {

    override fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun isDriveAuthorized(): Boolean {
        return driveService.isAuthorized()
    }

    override suspend fun uploadToAppData(
        backupPackage: BackupPackage,
        tempFile: File,
        metadata: Map<String, String>
    ): String {
        // Write package contents to the tempFile
        tempFile.writeBytes(backupPackage.encryptedPayload)
        
        // Custom App Data properties for tracking in Drive
        val driveMetadata = mutableMapOf<String, String>()
        driveMetadata["backupId"] = backupPackage.manifest.metadata.backupId
        driveMetadata["checksum"] = backupPackage.checksum
        driveMetadata["appVersion"] = backupPackage.manifest.metadata.appVersion
        driveMetadata["databaseVersion"] = backupPackage.manifest.metadata.databaseVersion.toString()
        driveMetadata["deviceName"] = backupPackage.manifest.metadata.deviceName
        driveMetadata["createdDate"] = backupPackage.manifest.metadata.createdDate.toString()
        
        // Merge with any custom metadata
        metadata.forEach { (key, value) ->
            driveMetadata[key] = value
        }

        val mimeType = "application/octet-stream"
        return driveService.uploadToAppData(tempFile, mimeType, driveMetadata)
    }
}
