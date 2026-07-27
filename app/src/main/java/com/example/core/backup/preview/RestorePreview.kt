package com.example.core.backup.preview

data class RestorePreview(
    val backupDate: Long,
    val backupTime: Long,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val backupSize: Long,
    val workspaceSummary: RestoreSummary,
    val impactSummary: RestoreImpact
)
