package com.example.core.sync.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupHistoryRepositoryImpl : BackupHistoryRepository {
    private val _history = MutableStateFlow<List<BackupHistoryItem>>(emptyList())
    
    override fun getHistory(): Flow<List<BackupHistoryItem>> = _history.asStateFlow()

    override suspend fun addRecord(item: BackupHistoryItem) {
        _history.update { current -> current + item }
    }

    override suspend fun updateRecord(item: BackupHistoryItem) {
        _history.update { current ->
            current.map { if (it.backupId == item.backupId) item else it }
        }
    }

    override suspend fun deleteRecord(backupId: String) {
        _history.update { current ->
            current.filter { it.backupId != backupId }
        }
    }
}
