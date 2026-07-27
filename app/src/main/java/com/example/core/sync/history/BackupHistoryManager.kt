package com.example.core.sync.history

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackupHistoryManager(
    private val repository: BackupHistoryRepository
) : BackupHistoryProvider {

    companion object {
        private const val TAG = "BackupHistoryManager"
    }

    override fun observeHistory(): Flow<BackupHistoryCollection> {
        return repository.getHistory().map { BackupHistoryCollection(it) }
    }

    override fun observeSortedHistory(ascending: Boolean): Flow<BackupHistoryCollection> {
        return repository.getHistory().map { list ->
            val sortedList = if (ascending) {
                list.sortedBy { it.date }
            } else {
                list.sortedByDescending { it.date }
            }
            BackupHistoryCollection(sortedList)
        }
    }

    override fun observeLatestBackup(): Flow<BackupHistoryItem?> {
        return repository.getHistory().map { list ->
            list.filter { it.status == BackupStatus.SUCCESS }.maxByOrNull { it.date }
        }
    }

    override fun observeStatistics(): Flow<BackupHistorySummary> {
        return repository.getHistory().map { list ->
            val totalSize = list.sumOf { it.backupSize }
            val lastDate = list.maxByOrNull { it.date }?.date
            val successfulCount = list.count { it.status == BackupStatus.SUCCESS }
            
            BackupHistorySummary(
                totalBackups = list.size,
                totalSize = totalSize,
                lastBackupDate = lastDate,
                successfulBackupsCount = successfulCount
            )
        }
    }

    override suspend fun addHistoryRecord(item: BackupHistoryItem) {

        repository.addRecord(item)
    }

    override suspend fun updateHistoryRecord(item: BackupHistoryItem) {

        repository.updateRecord(item)
    }

    override suspend fun deleteHistoryRecord(backupId: String) {

        repository.deleteRecord(backupId)
    }
}
