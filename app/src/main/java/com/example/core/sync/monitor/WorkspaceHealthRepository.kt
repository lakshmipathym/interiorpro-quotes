package com.example.core.sync.monitor

import kotlinx.coroutines.flow.Flow

interface WorkspaceHealthRepository {
    fun getWorkspaceHealthReport(): Flow<WorkspaceHealthReport>
    suspend fun updateDatabaseHealth(health: HealthIndicator)
    suspend fun updateBackupEngineHealth(health: HealthIndicator)
    suspend fun updateRestoreEngineHealth(health: HealthIndicator)
    suspend fun updateSyncEngineHealth(health: HealthIndicator)
    suspend fun updatePdfEngineHealth(health: HealthIndicator)
    suspend fun updateLocalStorageHealth(health: HealthIndicator)
    suspend fun updateGoogleDriveConnectivityHealth(health: HealthIndicator)
    suspend fun updateAppAndDatabaseVersion(appVersion: String, dbVersion: Int)
}
