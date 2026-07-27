package com.example.core.sync.history

import kotlinx.coroutines.flow.Flow

interface BackupHistoryRepository {
    fun getHistory(): Flow<List<BackupHistoryItem>>
    suspend fun addRecord(item: BackupHistoryItem)
    suspend fun updateRecord(item: BackupHistoryItem)
    suspend fun deleteRecord(backupId: String)
}
