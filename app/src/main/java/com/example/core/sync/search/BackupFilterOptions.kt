package com.example.core.sync.search

enum class BackupSortOption {
    NEWEST_FIRST,
    OLDEST_FIRST,
    LARGEST_SIZE,
    SMALLEST_SIZE,
    DEVICE_NAME,
    BACKUP_NAME
}

data class BackupFilterOptions(
    val isManual: Boolean = false,
    val isAutomatic: Boolean = false,
    val isLocal: Boolean = false,
    val isCloud: Boolean = false,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val status: String? = null,
    val isEncrypted: Boolean? = null,
    val dateStart: Long? = null,
    val dateEnd: Long? = null,
    val sortOption: BackupSortOption = BackupSortOption.NEWEST_FIRST
)
