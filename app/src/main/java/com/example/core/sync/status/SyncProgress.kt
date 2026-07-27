package com.example.core.sync.status

data class SyncProgress(
    val state: SyncState,
    val progressPercentage: Int,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val currentItem: String?,
    val startTime: Long? = null,
    val completionTime: Long? = null
)
