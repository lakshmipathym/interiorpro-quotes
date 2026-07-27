package com.example.core.backup.restore.execution

import com.example.core.backup.pkg.BackupPackage

interface RestoreExecutionRepository {
    suspend fun decryptAndExtract(backupPackage: BackupPackage, password: String): ByteArray
    suspend fun createLocalSafetyBackup(): String
    suspend fun beginTransaction(localBackupPath: String): RestoreTransaction
    suspend fun replaceWorkspaceData(transaction: RestoreTransaction, decryptedPayload: ByteArray): Boolean
    suspend fun verifyRestoredDatabase(transaction: RestoreTransaction): Boolean
    suspend fun commitTransaction(transaction: RestoreTransaction)
    suspend fun rollbackTransaction(transaction: RestoreTransaction): RestoreRollbackResult
}
