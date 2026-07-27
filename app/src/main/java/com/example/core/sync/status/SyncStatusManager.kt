package com.example.core.sync.status

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SyncStatusManager(
    private val repository: SyncStatusRepository
) : SyncStatusProvider {

    companion object {
        private const val TAG = "SyncStatusManager"
    }

    override fun observeState(): Flow<SyncState> = repository.getSyncState()
    override fun observeProgress(): Flow<SyncProgress> = repository.getSyncProgress()
    override fun observeStatistics(): Flow<SyncStatistics> = repository.getSyncStatistics()

    override suspend fun setState(state: SyncState) {

        repository.updateSyncState(state)
        
        val currentProgress = repository.getSyncProgress().first()
        val newStartTime = if (state == SyncState.PREPARING) System.currentTimeMillis() else currentProgress.startTime
        repository.updateSyncProgress(currentProgress.copy(state = state, startTime = newStartTime))
    }

    override suspend fun updateProgress(
        percentage: Int, 
        bytesTransferred: Long, 
        totalBytes: Long, 
        currentItem: String?
    ) {
        val currentProgress = repository.getSyncProgress().first()
        val progress = currentProgress.copy(
            progressPercentage = percentage,
            bytesTransferred = bytesTransferred,
            totalBytes = totalBytes,
            currentItem = currentItem
        )
        repository.updateSyncProgress(progress)
    }

    override suspend fun recordCompletion() {
        Log.i(TAG, "Recording sync completion")
        val endTime = System.currentTimeMillis()
        setState(SyncState.COMPLETED)
        
        val currentProgress = repository.getSyncProgress().first()
        repository.updateSyncProgress(currentProgress.copy(completionTime = endTime))
        
        val currentStats = repository.getSyncStatistics().first()
        val updatedStats = currentStats.copy(
            lastSuccessfulSync = endTime,
            totalSyncCount = currentStats.totalSyncCount + 1,
            totalBytesTransferred = currentStats.totalBytesTransferred + currentProgress.bytesTransferred
        )
        repository.updateSyncStatistics(updatedStats)
    }

    override suspend fun recordFailure(error: SyncError) {

        val endTime = System.currentTimeMillis()
        setState(SyncState.FAILED)
        
        val currentProgress = repository.getSyncProgress().first()
        repository.updateSyncProgress(currentProgress.copy(completionTime = endTime))
        
        val currentStats = repository.getSyncStatistics().first()
        val updatedStats = currentStats.copy(
            lastFailedSync = error.timestamp,
            lastError = error
        )
        repository.updateSyncStatistics(updatedStats)
    }

    override suspend fun generateSummary(): String {
        val stats = repository.getSyncStatistics().first()
        val state = repository.getSyncState().first()
        return "Sync Status: $state | Last Success: ${stats.lastSuccessfulSync ?: "Never"} | Last Failure: ${stats.lastFailedSync ?: "Never"} | Total Syncs: ${stats.totalSyncCount}"
    }
}
