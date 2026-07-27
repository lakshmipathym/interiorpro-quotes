package com.example.core.sync.migration

data class MigrationResult(
    val sessionId: String,
    val isSuccessful: Boolean,
    val summary: MigrationSummary?,
    val errorMessage: String? = null
)
