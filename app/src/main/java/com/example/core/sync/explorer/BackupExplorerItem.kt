package com.example.core.sync.explorer

data class BackupExplorerItem(
    val backupId: String,
    val name: String,
    val date: Long,
    val size: Long,
    val location: BackupLocation,
    val metadata: Map<String, String>,
    // Future properties
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false
)
