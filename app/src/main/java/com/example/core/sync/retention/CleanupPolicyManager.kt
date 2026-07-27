package com.example.core.sync.retention

import com.example.core.sync.history.BackupHistoryItem
import kotlinx.coroutines.flow.first

class CleanupPolicyManager(
    private val repository: RetentionRepository
) : CleanupPolicyProvider {

    override suspend fun analyzeBackups(items: List<BackupHistoryItem>): List<CleanupCandidate> {
        val policy = repository.getPolicy().first()
        val candidates = mutableListOf<CleanupCandidate>()
        
        // Sorting backups oldest first for potential cleanup evaluation
        val sortedItems = items.sortedBy { it.date }
        val latestBackup = items.maxByOrNull { it.date }

        val retainCount = policy.keepLastNBackups
        val keepIndices = if (retainCount != null) {
            sortedItems.indices.reversed().take(retainCount).toSet()
        } else {
            sortedItems.indices.toSet()
        }

        sortedItems.forEachIndexed { index, item ->
            var isCandidate = false
            var reason = ""

            if (retainCount != null && index !in keepIndices) {
                isCandidate = true
                reason = "Exceeds Keep Last N policy limit"
            }

            // Protect favorites
            if (policy.neverDeleteFavorites && item.isFavorite) {
                isCandidate = false
            }

            // Protect latest backup
            if (policy.neverDeleteLatest && item.backupId == latestBackup?.backupId) {
                isCandidate = false
            }

            // Time-based (Daily/Weekly/Monthly) protection rules would be evaluated here

            if (isCandidate) {
                candidates.add(
                    CleanupCandidate(
                        backupId = item.backupId,
                        name = item.backupName,
                        date = item.date,
                        size = item.backupSize,
                        reason = reason
                    )
                )
            }
        }

        return candidates
    }

    override suspend fun calculateStatistics(
        items: List<BackupHistoryItem>, 
        candidates: List<CleanupCandidate>
    ): RetentionStatistics {
        val candidateIds = candidates.map { it.backupId }.toSet()
        
        val totalBackups = items.size
        val activeBackups = items.count { !candidateIds.contains(it.backupId) }
        
        val totalStorage = items.sumOf { it.backupSize }
        val recoverableStorage = candidates.sumOf { it.size }

        return RetentionStatistics(
            totalBackups = totalBackups,
            activeBackups = activeBackups,
            cleanupCandidatesCount = candidates.size,
            storageUsed = totalStorage,
            storageRecoverable = recoverableStorage
        )
    }
}
