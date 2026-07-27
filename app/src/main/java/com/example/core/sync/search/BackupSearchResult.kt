package com.example.core.sync.search

data class BackupSearchResult<T>(
    val items: List<T>,
    val totalMatches: Int,
    val query: BackupSearchQuery? = null,
    val filters: BackupFilterOptions? = null
)
