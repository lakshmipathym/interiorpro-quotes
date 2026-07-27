package com.example.core.backup.restore.execution

sealed class RestoreRollbackResult {
    object Success : RestoreRollbackResult()
    data class Failure(val reason: String) : RestoreRollbackResult()
}
