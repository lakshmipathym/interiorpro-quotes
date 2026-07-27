package com.example.core.sync.retention

data class RetentionStatistics(
    val totalBackups: Int,
    val activeBackups: Int,
    val cleanupCandidatesCount: Int,
    val storageUsed: Long,
    val storageRecoverable: Long
)
