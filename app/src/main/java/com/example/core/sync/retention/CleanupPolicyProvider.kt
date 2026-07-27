package com.example.core.sync.retention

import com.example.core.sync.history.BackupHistoryItem

interface CleanupPolicyProvider {
    suspend fun analyzeBackups(items: List<BackupHistoryItem>): List<CleanupCandidate>
    suspend fun calculateStatistics(items: List<BackupHistoryItem>, candidates: List<CleanupCandidate>): RetentionStatistics
}
