package com.example.core.sync.retention

data class CleanupCandidate(
    val backupId: String,
    val name: String,
    val date: Long,
    val size: Long,
    val reason: String
)
