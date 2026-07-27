package com.example.core.sync.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupSearchRepositoryImpl : BackupSearchRepository {
    private val recentSearches = MutableStateFlow<List<String>>(emptyList())
    private val savedFilters = MutableStateFlow<List<BackupFilterOptions>>(emptyList())

    override fun getRecentSearches(): Flow<List<String>> = recentSearches.asStateFlow()
    override fun getSavedFilters(): Flow<List<BackupFilterOptions>> = savedFilters.asStateFlow()

    override suspend fun saveSearchQuery(query: String) {
        recentSearches.update { current ->
            val updated = current.toMutableList()
            updated.remove(query)
            updated.add(0, query)
            updated.take(10) // Retain last 10 searches
        }
    }

    override suspend fun saveFilter(filter: BackupFilterOptions) {
        savedFilters.update { current -> current + filter }
    }

    override suspend fun clearRecentSearches() {
        recentSearches.value = emptyList()
    }
}
