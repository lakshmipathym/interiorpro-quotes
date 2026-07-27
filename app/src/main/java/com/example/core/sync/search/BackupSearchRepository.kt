package com.example.core.sync.search

import kotlinx.coroutines.flow.Flow

interface BackupSearchRepository {
    fun getRecentSearches(): Flow<List<String>>
    fun getSavedFilters(): Flow<List<BackupFilterOptions>>
    
    suspend fun saveSearchQuery(query: String)
    suspend fun saveFilter(filter: BackupFilterOptions)
    suspend fun clearRecentSearches()
}
