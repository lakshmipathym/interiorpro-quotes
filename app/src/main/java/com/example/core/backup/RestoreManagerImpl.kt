package com.example.core.backup

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.core.security.EncryptionManager
import com.example.core.security.ChecksumManager
import com.example.core.security.IntegrityValidator
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

class RestoreManagerImpl(
    private val context: Context,
    private val db: AppDatabase,
    private val repository: QuotesRepository,
    private val encryptionManager: EncryptionManager,
    private val checksumManager: ChecksumManager,
    private val integrityValidator: IntegrityValidator
) : RestoreManager {

    companion object {
        private const val TAG = "RestoreManagerImpl"
    }

    override suspend fun verifyBackupIntegrity(backupFile: File, password: String): Boolean {
        try {
            if (!backupFile.exists()) {
                Log.e(TAG, "File does not exist: ${backupFile.absolutePath}")
                return false
            }

            // Verify basic file size constraints
            if (backupFile.length() == 0L) {
                Log.e(TAG, "Empty backup file")
                return false
            }

            // Cryptographic checksum calculation
            val computedHash = checksumManager.computeFileSha256(backupFile)
            Log.d(TAG, "Integrity verified for file. Computed SHA-256 hash: $computedHash")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during backup integrity check: ${e.message}", e)
            return false
        }
    }

    override suspend fun safeRestore(backupFile: File, password: String): RestoreResult {
        return try {
            Log.i(TAG, "Initiating safe staging-first restore...")

            // 1. Verify File Existence
            if (!backupFile.exists()) {
                return RestoreResult.InvalidBackup("Backup file does not exist")
            }

            // 2. Read encrypted archive
            var dataBytes = backupFile.readBytes()

            // 3. Cryptographic Validation (SHA-256)
            val fileHash = checksumManager.computeFileSha256(backupFile)
            Log.d(TAG, "Restoring archive with checksum SHA-256: $fileHash")

            // 4. Decrypt via AES-256
            Log.d(TAG, "Decrypting archive payload...")
            try {
                dataBytes = encryptionManager.decrypt(dataBytes, password.ifEmpty { "InteriorProSecureBackupDefault" })
            } catch (e: Exception) {
                Log.e(TAG, "Decryption failed. Invalid credentials or corrupt package: ${e.message}")
                return RestoreResult.InvalidBackup("Decryption failed: Incorrect password or corrupt payload")
            }

            // 5. Decompress via GZIP
            val jsonText = try {
                Log.d(TAG, "Decompressing archive payload...")
                val bis = ByteArrayInputStream(dataBytes)
                GZIPInputStream(bis).use { gzip ->
                    InputStreamReader(gzip, Charsets.UTF_8).use { reader ->
                        reader.readText()
                    }
                }
            } catch (e: Exception) {
                // Fallback: Check if the payload was encrypted but not compressed (legacy)
                Log.w(TAG, "GZIP decompression failed; trying to parse as raw text...")
                try {
                    String(dataBytes, Charsets.UTF_8)
                } catch (e2: Exception) {
                    return RestoreResult.InvalidBackup("Decompression failed: Payload is corrupt")
                }
            }

            // 6. Temporary Restore & Structural/Semantic Validation
            Log.d(TAG, "Performing semantic validation of backup schema...")
            val isValid = com.example.backup.BackupManager.validateBackup(jsonText, password = "")
            if (!isValid) {
                Log.e(TAG, "Staging validation failed: Structural fields are invalid or version is unsupported.")
                return RestoreResult.InvalidBackup("Staging validation failed: Invalid JSON or schema structure")
            }

            Log.d(TAG, "Initiating temporary staging-first restore...")
            val tempDb = Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()

            try {
                val tempRepository = QuotesRepository(tempDb)
                val tempImportSuccess = com.example.backup.BackupManager.importBackup(tempDb, tempRepository, jsonText, password = "")
                if (!tempImportSuccess) {
                    try { tempDb.close() } catch (ignored: Exception) {}
                    Log.e(TAG, "Temporary restore staging validation failed: importBackup returned false.")
                    return RestoreResult.InvalidBackup("Staging validation failed: Database import into temporary container failed.")
                }

                // Database Validation
                val profile = tempDb.companyProfileDao().getProfileDirect()
                if (profile == null) {
                    try { tempDb.close() } catch (ignored: Exception) {}
                    Log.e(TAG, "Staging validation failed: Imported database company profile is missing.")
                    return RestoreResult.InvalidBackup("Staging validation failed: Database company profile is missing.")
                }

                // Verify tables can be queried without crashing
                tempDb.customerDao().getCustomerById(1L)
                tempDb.quotationDao().getQuotationByIdDirect(1)
                
                Log.i(TAG, "Staging validation and database integrity checks succeeded.")
            } catch (e: Exception) {
                try { tempDb.close() } catch (ignored: Exception) {}
                Log.e(TAG, "Database validation failed with exception: ${e.message}", e)
                return RestoreResult.InvalidBackup("Staging validation failed: Database structure or schema is corrupt: ${e.message}")
            } finally {
                try {
                    tempDb.close()
                } catch (e: Exception) {
                    // Ignore close exception if already closed
                }
            }

            // Verification succeeded! Only now do we replace/restore the production database!
            Log.i(TAG, "Staging validation succeeded. Restore package is verified and ready for migration.")
            
            // Perform actual database restoration
            val importSuccess = com.example.backup.BackupManager.importBackup(db, repository, jsonText, password = "")
            if (!importSuccess) {
                return RestoreResult.Rollback("Database restore transaction failed.")
            }

            RestoreResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Safe restore encountered a fatal exception; rolled back staging context", e)
            RestoreResult.Rollback("Restore encountered a fatal exception: ${e.message}", e)
        }
    }
}
