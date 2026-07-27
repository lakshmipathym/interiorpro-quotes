package com.example.core.sync.explorer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupExplorerRepositoryImpl : BackupExplorerRepository {
    private val localBackups = MutableStateFlow<List<BackupExplorerItem>>(emptyList())
    private val cloudBackups = MutableStateFlow<List<BackupExplorerItem>>(emptyList())

    override fun getLocalBackups(): Flow<List<BackupExplorerItem>> = localBackups.asStateFlow()

    override fun getCloudBackups(): Flow<List<BackupExplorerItem>> = cloudBackups.asStateFlow()

    override suspend fun getBackupDetails(backupId: String): BackupExplorerItem? {
        val allBackups = localBackups.value + cloudBackups.value
        return allBackups.find { it.backupId == backupId }
    }

    override suspend fun getBackupMetadata(backupId: String): Map<String, String>? {
        return getBackupDetails(backupId)?.metadata
    }
}
