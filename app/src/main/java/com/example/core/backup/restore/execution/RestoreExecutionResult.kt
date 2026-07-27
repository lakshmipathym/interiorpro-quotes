package com.example.core.backup.restore.execution

sealed class RestoreExecutionResult {
    data class Success(val transactionId: String, val timestamp: Long) : RestoreExecutionResult()
    
    data class Failure(
        val reason: String,
        val rollbackResult: RestoreRollbackResult
    ) : RestoreExecutionResult()
}
