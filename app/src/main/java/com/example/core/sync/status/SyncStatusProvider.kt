package com.example.core.sync.status

import kotlinx.coroutines.flow.Flow

interface SyncStatusProvider {
    fun observeState(): Flow<SyncState>
    fun observeProgress(): Flow<SyncProgress>
    fun observeStatistics(): Flow<SyncStatistics>
    
    suspend fun setState(state: SyncState)
    suspend fun updateProgress(percentage: Int, bytesTransferred: Long, totalBytes: Long, currentItem: String? = null)
    suspend fun recordCompletion()
    suspend fun recordFailure(error: SyncError)
    suspend fun generateSummary(): String
}
