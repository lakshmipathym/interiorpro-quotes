package com.example.core.sync.search

import com.example.core.sync.history.BackupHistoryItem
import com.example.core.sync.explorer.BackupExplorerItem
import kotlinx.coroutines.flow.Flow

class BackupSearchManager(
    private val repository: BackupSearchRepository
) : BackupSearchProvider {

    override suspend fun searchHistory(items: List<BackupHistoryItem>, query: BackupSearchQuery): BackupSearchResult<BackupHistoryItem> {
        val result = items.filter { item ->
            val matchesQuery = query.query.isBlank() || 
                item.backupName.contains(query.query, ignoreCase = true) ||
                item.backupId.contains(query.query, ignoreCase = true) ||
                item.deviceName.contains(query.query, ignoreCase = true)
            
            val matchesDevice = query.deviceName.isNullOrBlank() || item.deviceName.equals(query.deviceName, ignoreCase = true)
            val matchesAppVersion = query.appVersion.isNullOrBlank() || item.appVersion == query.appVersion
            val matchesDbVersion = query.databaseVersion == null || item.databaseVersion == query.databaseVersion
            val matchesType = query.type.isNullOrBlank() || item.type.name.equals(query.type, ignoreCase = true)
            
            val matchesDateStart = query.dateRangeStart == null || item.date >= query.dateRangeStart
            val matchesDateEnd = query.dateRangeEnd == null || item.date <= query.dateRangeEnd
            
            matchesQuery && matchesDevice && matchesAppVersion && matchesDbVersion && matchesType && matchesDateStart && matchesDateEnd
        }
        
        return BackupSearchResult(
            items = result,
            totalMatches = result.size,
            query = query,
            filters = null
        )
    }

    override suspend fun searchExplorer(items: List<BackupExplorerItem>, query: BackupSearchQuery): BackupSearchResult<BackupExplorerItem> {
        val result = items.filter { item ->
            val matchesQuery = query.query.isBlank() || 
                item.name.contains(query.query, ignoreCase = true) ||
                item.backupId.contains(query.query, ignoreCase = true)
            
            val deviceName = item.metadata["deviceName"]
            val appVersion = item.metadata["appVersion"]
            val dbVersion = item.metadata["databaseVersion"]?.toIntOrNull()
            val backupType = item.metadata["type"]

            val matchesDevice = query.deviceName.isNullOrBlank() || deviceName.equals(query.deviceName, ignoreCase = true)
            val matchesAppVersion = query.appVersion.isNullOrBlank() || appVersion == query.appVersion
            val matchesDbVersion = query.databaseVersion == null || dbVersion == query.databaseVersion
            val matchesType = query.type.isNullOrBlank() || backupType.equals(query.type, ignoreCase = true)
            
            val matchesDateStart = query.dateRangeStart == null || item.date >= query.dateRangeStart
            val matchesDateEnd = query.dateRangeEnd == null || item.date <= query.dateRangeEnd
            
            matchesQuery && matchesDevice && matchesAppVersion && matchesDbVersion && matchesType && matchesDateStart && matchesDateEnd
        }
        
        return BackupSearchResult(
            items = result,
            totalMatches = result.size,
            query = query,
            filters = null
        )
    }

    override fun observeRecentSearches(): Flow<List<String>> = repository.getRecentSearches()

    override suspend fun saveSearch(query: String) {
        if (query.isNotBlank()) {
            repository.saveSearchQuery(query)
        }
    }

    override suspend fun clearHistory() {
        repository.clearRecentSearches()
    }
}
