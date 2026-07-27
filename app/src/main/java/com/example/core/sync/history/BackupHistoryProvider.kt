package com.example.core.sync.history

import kotlinx.coroutines.flow.Flow

interface BackupHistoryProvider {
    fun observeHistory(): Flow<BackupHistoryCollection>
    fun observeSortedHistory(ascending: Boolean = false): Flow<BackupHistoryCollection>
    fun observeLatestBackup(): Flow<BackupHistoryItem?>
    fun observeStatistics(): Flow<BackupHistorySummary>
    
    suspend fun addHistoryRecord(item: BackupHistoryItem)
    suspend fun updateHistoryRecord(item: BackupHistoryItem)
    suspend fun deleteHistoryRecord(backupId: String)
}
