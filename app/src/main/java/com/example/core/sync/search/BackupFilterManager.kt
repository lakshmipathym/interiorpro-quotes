package com.example.core.sync.search

import com.example.core.sync.history.BackupHistoryItem
import com.example.core.sync.explorer.BackupExplorerItem
import com.example.core.sync.history.BackupLocation as HistoryLocation
import com.example.core.sync.explorer.BackupLocation as ExplorerLocation
import com.example.core.sync.history.BackupType
import kotlinx.coroutines.flow.Flow

class BackupFilterManager(
    private val repository: BackupSearchRepository
) : BackupFilterProvider {

    override suspend fun filterHistory(items: List<BackupHistoryItem>, options: BackupFilterOptions): BackupSearchResult<BackupHistoryItem> {
        val filtered = items.filter { item ->
            val typeMatch = if (options.isManual && options.isAutomatic) {
                true
            } else if (options.isManual) {
                item.type == BackupType.MANUAL
            } else if (options.isAutomatic) {
                item.type == BackupType.AUTOMATIC
            } else true
            
            val locationMatch = if (options.isLocal && options.isCloud) {
                true
            } else if (options.isLocal) {
                item.location == HistoryLocation.LOCAL
            } else if (options.isCloud) {
                item.location == HistoryLocation.CLOUD
            } else true

            val matchesMinSize = options.minSize == null || item.backupSize >= options.minSize
            val matchesMaxSize = options.maxSize == null || item.backupSize <= options.maxSize
            
            val matchesStatus = options.status.isNullOrBlank() || item.status.name.equals(options.status, ignoreCase = true)
            val matchesEncryption = options.isEncrypted == null || item.isEncrypted == options.isEncrypted
            
            val matchesDateStart = options.dateStart == null || item.date >= options.dateStart
            val matchesDateEnd = options.dateEnd == null || item.date <= options.dateEnd

            typeMatch && locationMatch && matchesMinSize && matchesMaxSize && matchesStatus && matchesEncryption && matchesDateStart && matchesDateEnd
        }

        val sorted = when (options.sortOption) {
            BackupSortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.date }
            BackupSortOption.OLDEST_FIRST -> filtered.sortedBy { it.date }
            BackupSortOption.LARGEST_SIZE -> filtered.sortedByDescending { it.backupSize }
            BackupSortOption.SMALLEST_SIZE -> filtered.sortedBy { it.backupSize }
            BackupSortOption.DEVICE_NAME -> filtered.sortedBy { it.deviceName }
            BackupSortOption.BACKUP_NAME -> filtered.sortedBy { it.backupName }
        }

        return BackupSearchResult(
            items = sorted,
            totalMatches = sorted.size,
            query = null,
            filters = options
        )
    }

    override suspend fun filterExplorer(items: List<BackupExplorerItem>, options: BackupFilterOptions): BackupSearchResult<BackupExplorerItem> {
        val filtered = items.filter { item ->
            val backupType = item.metadata["type"]
            val typeMatch = if (options.isManual && options.isAutomatic) {
                true
            } else if (options.isManual) {
                backupType.equals("MANUAL", ignoreCase = true)
            } else if (options.isAutomatic) {
                backupType.equals("AUTOMATIC", ignoreCase = true)
            } else true
            
            val locationMatch = if (options.isLocal && options.isCloud) {
                true
            } else if (options.isLocal) {
                item.location == ExplorerLocation.LOCAL
            } else if (options.isCloud) {
                item.location == ExplorerLocation.GOOGLE_DRIVE
            } else true

            val matchesMinSize = options.minSize == null || item.size >= options.minSize
            val matchesMaxSize = options.maxSize == null || item.size <= options.maxSize
            
            val backupStatus = item.metadata["status"]
            val matchesStatus = options.status.isNullOrBlank() || backupStatus.equals(options.status, ignoreCase = true)
            
            val isEncrypted = item.metadata["isEncrypted"]?.toBoolean() ?: false
            val matchesEncryption = options.isEncrypted == null || isEncrypted == options.isEncrypted
            
            val matchesDateStart = options.dateStart == null || item.date >= options.dateStart
            val matchesDateEnd = options.dateEnd == null || item.date <= options.dateEnd

            typeMatch && locationMatch && matchesMinSize && matchesMaxSize && matchesStatus && matchesEncryption && matchesDateStart && matchesDateEnd
        }

        val sorted = when (options.sortOption) {
            BackupSortOption.NEWEST_FIRST -> filtered.sortedByDescending { it.date }
            BackupSortOption.OLDEST_FIRST -> filtered.sortedBy { it.date }
            BackupSortOption.LARGEST_SIZE -> filtered.sortedByDescending { it.size }
            BackupSortOption.SMALLEST_SIZE -> filtered.sortedBy { it.size }
            BackupSortOption.DEVICE_NAME -> filtered.sortedBy { item -> item.metadata["deviceName"] ?: "" }
            BackupSortOption.BACKUP_NAME -> filtered.sortedBy { it.name }
        }

        return BackupSearchResult(
            items = sorted,
            totalMatches = sorted.size,
            query = null,
            filters = options
        )
    }

    override fun observeSavedFilters(): Flow<List<BackupFilterOptions>> = repository.getSavedFilters()

    override suspend fun saveFilter(options: BackupFilterOptions) {
        repository.saveFilter(options)
    }
}
