package com.example.core.backup.sandbox

import android.util.Log
import com.example.core.backup.pkg.BackupPackage
import com.example.core.security.ChecksumManager

class RestoreSandboxManager(
    private val repository: RestoreSandboxRepository,
    private val checksumManager: ChecksumManager
) : RestoreSandboxProvider {

    companion object {
        private const val TAG = "RestoreSandboxManager"
    }

    override suspend fun validateBackupInSandbox(
        backupPackage: BackupPackage,
        password: String
    ): SandboxValidationResult {
        Log.i(TAG, "Creating temporary restore sandbox...")
        val session = repository.createSandboxWorkspace()

        return try {
            // 1. Verify Checksum
            val computedChecksum = checksumManager.computeSha256(backupPackage.encryptedPayload)
            if (computedChecksum != backupPackage.checksum) {

                return SandboxValidationResult.ChecksumMismatch(backupPackage.checksum, computedChecksum)
            }

            // 2. Extract and Decrypt

            val decryptedPayload = try {
                repository.extractAndDecrypt(session, backupPackage.encryptedPayload, password)
            } catch (e: Exception) {

                return SandboxValidationResult.DecryptionFailed("Unknown decryption error")
            }

            // 3. Validate Schema & Compare Metadata

            val isSchemaValid = repository.verifySchema(decryptedPayload)
            if (!isSchemaValid) {

                return SandboxValidationResult.InvalidSchema("Decrypted payload does not match expected database schema")
            }

            Log.i(TAG, "Sandbox validation completed successfully")
            SandboxValidationResult.Success("Backup successfully validated in sandbox")
        } catch (e: Exception) {

            SandboxValidationResult.UnknownError("Unknown error")
        } finally {
            Log.i(TAG, "Destroying temporary restore sandbox...")
            repository.destroySandbox(session)
        }
    }
}
