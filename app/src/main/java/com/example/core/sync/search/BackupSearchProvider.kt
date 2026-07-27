package com.example.core.sync.search

import com.example.core.sync.history.BackupHistoryItem
import com.example.core.sync.explorer.BackupExplorerItem
import kotlinx.coroutines.flow.Flow

interface BackupSearchProvider {
    suspend fun searchHistory(items: List<BackupHistoryItem>, query: BackupSearchQuery): BackupSearchResult<BackupHistoryItem>
    suspend fun searchExplorer(items: List<BackupExplorerItem>, query: BackupSearchQuery): BackupSearchResult<BackupExplorerItem>
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun saveSearch(query: String)
    suspend fun clearHistory()
}
