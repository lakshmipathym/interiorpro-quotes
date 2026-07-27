package com.example.core.backup.pkg

import android.content.Context
import android.os.Build
import com.example.core.device.DeviceManager
import java.util.UUID

class MetadataGenerator(
    private val context: Context,
    private val deviceManager: DeviceManager
) {
    fun generateMetadata(
        backupVersion: Int = 1,
        databaseVersion: Int = 6,
        appVersion: String = "1.5",
        compressionType: String = "GZIP",
        encryptionType: String = "AES-256",
        checksumType: String = "SHA-256",
        extra: Map<String, String> = emptyMap()
    ): BackupMetadata {
        val backupId = UUID.randomUUID().toString()
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        
        return BackupMetadata(
            backupId = backupId,
            backupVersion = backupVersion,
            createdDate = System.currentTimeMillis(),
            deviceName = deviceName,
            appVersion = appVersion,
            databaseVersion = databaseVersion,
            compressionType = compressionType,
            encryptionType = encryptionType,
            checksumType = checksumType,
            extraMetadata = extra
        )
    }
}
