package com.example.core.backup.discovery

data class CloudBackupInfo(
    val id: String,
    val fileName: String,
    val backupId: String,
    val backupVersion: Int,
    val createdDate: Long,
    val sizeBytes: Long,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val checksum: String,
    val extraMetadata: Map<String, String> = emptyMap()
)
