package com.example.core.sync.status

import kotlinx.coroutines.flow.Flow

interface SyncStatusRepository {
    fun getSyncState(): Flow<SyncState>
    fun getSyncProgress(): Flow<SyncProgress>
    fun getSyncStatistics(): Flow<SyncStatistics>
    
    suspend fun updateSyncState(state: SyncState)
    suspend fun updateSyncProgress(progress: SyncProgress)
    suspend fun updateSyncStatistics(statistics: SyncStatistics)
}
