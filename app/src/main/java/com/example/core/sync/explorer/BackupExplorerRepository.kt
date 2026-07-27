package com.example.core.sync.explorer

import kotlinx.coroutines.flow.Flow

interface BackupExplorerRepository {
    fun getLocalBackups(): Flow<List<BackupExplorerItem>>
    fun getCloudBackups(): Flow<List<BackupExplorerItem>>
    suspend fun getBackupDetails(backupId: String): BackupExplorerItem?
    suspend fun getBackupMetadata(backupId: String): Map<String, String>?
}
