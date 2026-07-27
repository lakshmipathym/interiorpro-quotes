package com.example.core.sync.search

import com.example.core.sync.history.BackupHistoryItem
import com.example.core.sync.explorer.BackupExplorerItem
import kotlinx.coroutines.flow.Flow

interface BackupFilterProvider {
    suspend fun filterHistory(items: List<BackupHistoryItem>, options: BackupFilterOptions): BackupSearchResult<BackupHistoryItem>
    suspend fun filterExplorer(items: List<BackupExplorerItem>, options: BackupFilterOptions): BackupSearchResult<BackupExplorerItem>
    fun observeSavedFilters(): Flow<List<BackupFilterOptions>>
    suspend fun saveFilter(options: BackupFilterOptions)
}
