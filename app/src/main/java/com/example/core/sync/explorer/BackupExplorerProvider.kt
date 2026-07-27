package com.example.core.sync.explorer

import kotlinx.coroutines.flow.Flow

interface BackupExplorerProvider {
    fun observeLocalBackups(): Flow<BackupExplorerCollection>
    fun observeCloudBackups(): Flow<BackupExplorerCollection>
    fun observeAllBackups(): Flow<BackupExplorerCollection>
    
    fun observeGroupedBackups(): Flow<Map<BackupCategory, List<BackupExplorerItem>>>
    fun observeSortedBackups(ascending: Boolean = false): Flow<BackupExplorerCollection>
    
    suspend fun getBackupDetails(backupId: String): BackupExplorerItem?
    suspend fun getBackupMetadata(backupId: String): Map<String, String>?
    suspend fun calculateTotalStorageUsed(): Long
}
