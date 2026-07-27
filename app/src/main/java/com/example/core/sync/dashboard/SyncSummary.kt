package com.example.core.sync.dashboard

enum class SyncStatus {
    IDLE, UPLOADING, DOWNLOADING, SYNC_COMPLETE, SYNC_ERROR
}

data class SyncSummary(
    val syncStatus: SyncStatus,
    val lastBackupTime: Long?,
    val lastBackupStatus: String,
    val lastBackupSize: Long,
    val connectedAccount: String?,
    val isAccountConnected: Boolean,
    val customerCount: Int,
    val quotationCount: Int,
    val masterRecordsCount: Int,
    val databaseVersion: Int
)
