package com.example.core.sync.history

data class BackupHistorySummary(
    val totalBackups: Int,
    val totalSize: Long,
    val lastBackupDate: Long?,
    val successfulBackupsCount: Int
)
