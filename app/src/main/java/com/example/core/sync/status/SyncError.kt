package com.example.core.sync.status

data class SyncError(
    val code: String,
    val message: String,
    val timestamp: Long,
    val exception: Throwable?
)
