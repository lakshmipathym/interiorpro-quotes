package com.example.core.sync.migration

import kotlinx.coroutines.flow.Flow

interface WorkspaceMigrationProvider {
    suspend fun startMigration(request: MigrationRequest): MigrationSession
    suspend fun generatePreview(sessionId: String): MigrationSummary
    suspend fun confirmMigration(sessionId: String)
    suspend fun cancelMigration(sessionId: String)
    suspend fun rollbackMigration(sessionId: String)
    suspend fun resumeMigration(sessionId: String)
    fun observeMigrationSession(): Flow<MigrationSession?>
    
    // Future features architecture
    suspend fun prepareQrMigration()
    suspend fun prepareNearbyDeviceMigration()
    suspend fun prepareWifiDirectMigration()
    suspend fun prepareUsbMigration()
    suspend fun prepareCloudMigration()
}
