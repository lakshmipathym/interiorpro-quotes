package com.example.core.sync.history

enum class BackupType {
    MANUAL,
    AUTOMATIC
}

enum class BackupStatus {
    SUCCESS,
    FAILED,
    IN_PROGRESS
}

enum class BackupLocation {
    LOCAL,
    CLOUD
}

data class BackupHistoryItem(
    val backupId: String,
    val backupName: String,
    val date: Long,
    val time: Long,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val backupSize: Long,
    val type: BackupType,
    val status: BackupStatus,
    val isEncrypted: Boolean,
    val isChecksumValid: Boolean,
    // Future properties
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val rating: Int? = null,
    val location: BackupLocation = BackupLocation.LOCAL
)
