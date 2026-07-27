package com.example.core.backup.restore.execution

import android.util.Log
import com.example.core.backup.restore.ValidationResult
import com.example.core.security.ChecksumManager
import kotlinx.coroutines.CancellationException

class RestoreExecutionManager(
    private val repository: RestoreExecutionRepository,
    private val checksumManager: ChecksumManager
) : RestoreExecutionProvider {

    companion object {
        private const val TAG = "RestoreExecutionManager"
    }

    override suspend fun executeRestore(request: RestoreExecutionRequest): RestoreExecutionResult {
        Log.i(TAG, "Starting Safe Restore Execution Engine...")

        try {
            // 1. Verify Restore Preview

            if (request.preview.backupSize != request.backupPackage.encryptedPayload.size.toLong()) {

                return RestoreExecutionResult.Failure(
                    "Preview mismatch: package size differs",
                    RestoreRollbackResult.Success
                )
            }

            // 2. Verify Validation Result

            if (request.validationResult !is ValidationResult.Compatible) {

                return RestoreExecutionResult.Failure(
                    "Backup validation is not compatible for execution",
                    RestoreRollbackResult.Success
                )
            }

            // 3. Verify Checksum

            val computedChecksum = checksumManager.computeSha256(request.backupPackage.encryptedPayload)
            if (computedChecksum != request.backupPackage.checksum) {

                return RestoreExecutionResult.Failure(
                    "Checksum verification failed prior to execution",
                    RestoreRollbackResult.Success
                )
            }

            // 4. Verify Encryption & Extract

            val decryptedPayload = try {
                repository.decryptAndExtract(request.backupPackage, request.passwordUsed)
            } catch (e: Exception) {

                return RestoreExecutionResult.Failure(
                    "Decryption or extraction failed",
                    RestoreRollbackResult.Success
                )
            }

            // 5. Create Local Backup Before Restore

            val localBackupPath = try {
                repository.createLocalSafetyBackup()
            } catch (e: Exception) {

                return RestoreExecutionResult.Failure(
                    "Safety backup creation failed. Aborting restore.",
                    RestoreRollbackResult.Success
                )
            }

            // 6. Begin Restore Transaction

            val transaction = repository.beginTransaction(localBackupPath)

            try {
                // 7. Replace Workspace Data

                val replaced = repository.replaceWorkspaceData(transaction, decryptedPayload)
                if (!replaced) {
                    throw IllegalStateException("Workspace replacement operation failed")
                }

                // 8. Verify Restored Database

                val verified = repository.verifyRestoredDatabase(transaction)
                if (!verified) {
                    throw IllegalStateException("Restored database failed integrity verification")
                }

                // 9. Commit Transaction

                repository.commitTransaction(transaction)

                Log.i(TAG, "Safe restore executed successfully.")
                return RestoreExecutionResult.Success(transaction.transactionId, transaction.timestamp)

            } catch (e: Exception) {
                if (e is CancellationException) throw e

                val rollbackResult = repository.rollbackTransaction(transaction)
                return RestoreExecutionResult.Failure(
                    reason = "Transaction failed",
                    rollbackResult = rollbackResult
                )
            }

        } catch (e: CancellationException) {
            Log.w(TAG, "Restore execution was cancelled")
            throw e
        } catch (e: Exception) {

            return RestoreExecutionResult.Failure(
                reason = "Unknown error",
                rollbackResult = RestoreRollbackResult.Success // Failed outside transaction
            )
        }
    }
}
