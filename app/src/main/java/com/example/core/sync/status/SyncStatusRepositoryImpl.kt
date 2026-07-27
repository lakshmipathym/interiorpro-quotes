package com.example.core.sync.status

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncStatusRepositoryImpl : SyncStatusRepository {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val _syncProgress = MutableStateFlow(SyncProgress(SyncState.IDLE, 0, 0L, 0L, null))
    private val _syncStatistics = MutableStateFlow(SyncStatistics(null, null, null, 0, 0L))

    override fun getSyncState(): Flow<SyncState> = _syncState.asStateFlow()
    override fun getSyncProgress(): Flow<SyncProgress> = _syncProgress.asStateFlow()
    override fun getSyncStatistics(): Flow<SyncStatistics> = _syncStatistics.asStateFlow()

    override suspend fun updateSyncState(state: SyncState) {
        _syncState.value = state
    }

    override suspend fun updateSyncProgress(progress: SyncProgress) {
        _syncProgress.value = progress
    }

    override suspend fun updateSyncStatistics(statistics: SyncStatistics) {
        _syncStatistics.value = statistics
    }
}
