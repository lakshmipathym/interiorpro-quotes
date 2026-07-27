package com.example.core.backup.pkg

data class BackupMetadata(
    val backupId: String,
    val backupVersion: Int,
    val createdDate: Long,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val compressionType: String,
    val encryptionType: String,
    val checksumType: String,
    val extraMetadata: Map<String, String> = emptyMap()
)
