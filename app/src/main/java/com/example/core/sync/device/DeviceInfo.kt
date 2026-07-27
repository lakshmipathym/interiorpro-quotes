package com.example.core.sync.device

data class DeviceInfo(
    val deviceId: String,
    val installId: String,
    val deviceName: String,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String,
    val databaseVersion: Int,
    val registrationDate: Long,
    val lastSeen: Long,
    val lastBackup: Long?,
    val lastSync: Long?,
    val backupSize: Long,
    val status: DeviceStatus,
    val isHealthy: Boolean
)
