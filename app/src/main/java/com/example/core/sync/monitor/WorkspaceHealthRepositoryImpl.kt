package com.example.core.sync.monitor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WorkspaceHealthRepositoryImpl : WorkspaceHealthRepository {
    private val _healthReport = MutableStateFlow(
        WorkspaceHealthReport(
            databaseHealth = HealthIndicator.UNKNOWN,
            backupEngine = HealthIndicator.UNKNOWN,
            restoreEngine = HealthIndicator.UNKNOWN,
            syncEngine = HealthIndicator.UNKNOWN,
            pdfEngine = HealthIndicator.UNKNOWN,
            localStorage = HealthIndicator.UNKNOWN,
            googleDriveConnectivity = HealthIndicator.UNKNOWN,
            appVersion = "1.0.0",
            databaseVersion = 1
        )
    )

    override fun getWorkspaceHealthReport(): Flow<WorkspaceHealthReport> = _healthReport.asStateFlow()

    override suspend fun updateDatabaseHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(databaseHealth = health) }
    }

    override suspend fun updateBackupEngineHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(backupEngine = health) }
    }

    override suspend fun updateRestoreEngineHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(restoreEngine = health) }
    }

    override suspend fun updateSyncEngineHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(syncEngine = health) }
    }

    override suspend fun updatePdfEngineHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(pdfEngine = health) }
    }

    override suspend fun updateLocalStorageHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(localStorage = health) }
    }

    override suspend fun updateGoogleDriveConnectivityHealth(health: HealthIndicator) {
        _healthReport.update { it.copy(googleDriveConnectivity = health) }
    }

    override suspend fun updateAppAndDatabaseVersion(appVersion: String, dbVersion: Int) {
        _healthReport.update { it.copy(appVersion = appVersion, databaseVersion = dbVersion) }
    }
}
