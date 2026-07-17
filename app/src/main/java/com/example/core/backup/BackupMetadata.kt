package com.example.core.backup

/**
 * BackupMetadata keeps track of crucial system information, checksum validation, and security states
 * to protect enterprise restore operations.
 */
data class BackupMetadata(
    val version: Int,
    val timestamp: Long,
    val checksum: String,
    val databaseVersion: Int,
    val appVersion: String,
    val deviceId: String,
    val deviceName: String,
    val isEncrypted: Boolean,
    val isCompressed: Boolean,
    val recordCount: Int,
    val extraMetadata: Map<String, String> = emptyMap()
)
