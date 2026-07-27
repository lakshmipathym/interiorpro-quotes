package com.example.core.sync.explorer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class BackupExplorerManager(
    private val repository: BackupExplorerRepository
) : BackupExplorerProvider {

    override fun observeLocalBackups(): Flow<BackupExplorerCollection> {
        return repository.getLocalBackups().map { BackupExplorerCollection(it) }
    }

    override fun observeCloudBackups(): Flow<BackupExplorerCollection> {
        return repository.getCloudBackups().map { BackupExplorerCollection(it) }
    }

    override fun observeAllBackups(): Flow<BackupExplorerCollection> {
        return combine(repository.getLocalBackups(), repository.getCloudBackups()) { local, cloud ->
            BackupExplorerCollection(local + cloud)
        }
    }

    override fun observeGroupedBackups(): Flow<Map<BackupCategory, List<BackupExplorerItem>>> {
        return observeAllBackups().map { collection ->
            collection.items.groupBy { item -> categorizeBackup(item.date) }
        }
    }

    override fun observeSortedBackups(ascending: Boolean): Flow<BackupExplorerCollection> {
        return observeAllBackups().map { collection ->
            val sorted = if (ascending) {
                collection.items.sortedBy { it.date }
            } else {
                collection.items.sortedByDescending { it.date }
            }
            BackupExplorerCollection(sorted)
        }
    }

    override suspend fun getBackupDetails(backupId: String): BackupExplorerItem? {
        return repository.getBackupDetails(backupId)
    }

    override suspend fun getBackupMetadata(backupId: String): Map<String, String>? {
        return repository.getBackupMetadata(backupId)
    }

    override suspend fun calculateTotalStorageUsed(): Long {
        val local = repository.getLocalBackups().first()
        val cloud = repository.getCloudBackups().first()
        return local.sumOf { it.size } + cloud.sumOf { it.size }
    }

    private fun categorizeBackup(date: Long): BackupCategory {
        val now = Calendar.getInstance()
        val backupDate = Calendar.getInstance().apply { timeInMillis = date }
        
        val sameYear = now.get(Calendar.YEAR) == backupDate.get(Calendar.YEAR)
        val sameMonth = now.get(Calendar.MONTH) == backupDate.get(Calendar.MONTH)
        val sameDay = now.get(Calendar.DAY_OF_YEAR) == backupDate.get(Calendar.DAY_OF_YEAR)
        
        now.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = now.get(Calendar.YEAR) == backupDate.get(Calendar.YEAR) &&
                          now.get(Calendar.DAY_OF_YEAR) == backupDate.get(Calendar.DAY_OF_YEAR)
        
        now.add(Calendar.DAY_OF_YEAR, 1) // reset
        
        val daysDiff = (now.timeInMillis - date) / (1000 * 60 * 60 * 24)

        return when {
            sameYear && sameDay -> BackupCategory.TODAY
            isYesterday -> BackupCategory.YESTERDAY
            daysDiff <= 7 -> BackupCategory.LAST_7_DAYS
            sameYear && sameMonth -> BackupCategory.THIS_MONTH
            else -> BackupCategory.OLDER
        }
    }
}
