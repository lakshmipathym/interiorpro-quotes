package com.example.core.sync.migration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceMigrationRepositoryImpl : WorkspaceMigrationRepository {
    private val _currentSession = MutableStateFlow<MigrationSession?>(null)

    override fun observeCurrentSession(): Flow<MigrationSession?> = _currentSession.asStateFlow()

    override suspend fun saveSession(session: MigrationSession) {
        _currentSession.value = session
    }

    override suspend fun updateSessionState(sessionId: String, state: MigrationState, progress: MigrationProgress) {
        _currentSession.value = _currentSession.value?.takeIf { it.sessionId == sessionId }?.copy(
            state = state,
            progress = progress
        )
    }

    override suspend fun clearSession() {
        _currentSession.value = null
    }
}
