package com.example.core.sync.migration

import kotlinx.coroutines.flow.Flow

interface WorkspaceMigrationRepository {
    fun observeCurrentSession(): Flow<MigrationSession?>
    suspend fun saveSession(session: MigrationSession)
    suspend fun updateSessionState(sessionId: String, state: MigrationState, progress: MigrationProgress)
    suspend fun clearSession()
}
