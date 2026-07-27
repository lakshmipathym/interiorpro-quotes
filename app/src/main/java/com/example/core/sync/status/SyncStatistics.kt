package com.example.core.sync.status

data class SyncStatistics(
    val lastSuccessfulSync: Long?,
    val lastFailedSync: Long?,
    val lastError: SyncError?,
    val totalSyncCount: Int,
    val totalBytesTransferred: Long
)
