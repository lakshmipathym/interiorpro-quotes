package com.example.core.backup.restore.execution

interface RestoreExecutionProvider {
    suspend fun executeRestore(request: RestoreExecutionRequest): RestoreExecutionResult
}
