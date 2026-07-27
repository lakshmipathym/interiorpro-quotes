package com.example.core.sync.migration

enum class MigrationState {
    INITIALIZED,
    VALIDATING_PACKAGE,
    VALIDATING_COMPATIBILITY,
    CREATING_SANDBOX,
    PREVIEW_GENERATED,
    WAITING_FOR_CONFIRMATION,
    MIGRATING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    ROLLED_BACK
}

data class MigrationSession(
    val sessionId: String,
    val request: MigrationRequest,
    val state: MigrationState,
    val progress: MigrationProgress,
    val errorMessage: String? = null
)
