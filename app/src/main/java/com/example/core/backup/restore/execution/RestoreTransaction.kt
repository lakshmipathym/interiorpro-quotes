package com.example.core.backup.restore.execution

data class RestoreTransaction(
    val transactionId: String,
    val timestamp: Long,
    val localSafetyBackupPath: String,
    val status: TransactionStatus
)

enum class TransactionStatus {
    PENDING,
    IN_PROGRESS,
    COMMITTED,
    ROLLED_BACK,
    FAILED
}
