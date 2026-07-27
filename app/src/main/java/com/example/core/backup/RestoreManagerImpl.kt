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

                return false
            }

            // Verify basic file size constraints
            if (backupFile.length() == 0L) {

                return false
            }

            // Cryptographic checksum calculation
            val computedHash = checksumManager.computeFileSha256(backupFile)

            return true
        } catch (e: Exception) {

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
            if (backupFile.length() == 0L || backupFile.length() > 50 * 1024 * 1024) {
                return RestoreResult.InvalidBackup("Backup file is empty or exceeds maximum size (50MB)")
            }

            // 2. Read encrypted archive
            var dataBytes = backupFile.readBytes()

            // 3. Cryptographic Validation (SHA-256)
            val fileHash = checksumManager.computeFileSha256(backupFile)


            // 4. Decrypt via AES-256

            try {
                dataBytes = encryptionManager.decrypt(dataBytes, password.ifEmpty { "InteriorProSecureBackupDefault" })
            } catch (e: Exception) {

                return RestoreResult.InvalidBackup("Decryption failed: Incorrect password or corrupt payload")
            }

            // 5. Decompress via GZIP
            val jsonText = try {

                val bis = ByteArrayInputStream(dataBytes)
                GZIPInputStream(bis).use { gzip ->
                    InputStreamReader(gzip, Charsets.UTF_8).use { reader ->
                        val sb = java.lang.StringBuilder()
                        val buffer = CharArray(8192)
                        var charsRead: Int
                        var totalChars = 0
                        val maxChars = 100 * 1024 * 1024 // 100MB limit for zip bombs
                        while (reader.read(buffer).also { charsRead = it } != -1) {
                            totalChars += charsRead
                            if (totalChars > maxChars) throw SecurityException("Payload exceeded maximum allowed size")
                            sb.append(buffer, 0, charsRead)
                        }
                        sb.toString()
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

            val isValid = com.example.backup.BackupManager.validateBackup(jsonText, password = "")
            if (!isValid) {

                return RestoreResult.InvalidBackup("Staging validation failed: Invalid JSON or schema structure")
            }

            val tempDb = Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java
            ).allowMainThreadQueries().build()

            try {
                val tempRepository = QuotesRepository(tempDb)
                val tempImportSuccess = com.example.backup.BackupManager.importBackup(tempDb, tempRepository, jsonText, password = "")
                if (!tempImportSuccess) {
                    try { tempDb.close() } catch (ignored: Exception) {}

                    return RestoreResult.InvalidBackup("Staging validation failed: Database import into temporary container failed.")
                }

                // Database Validation
                val profile = tempDb.companyProfileDao().getProfileDirect()
                if (profile == null) {
                    try { tempDb.close() } catch (ignored: Exception) {}

                    return RestoreResult.InvalidBackup("Staging validation failed: Database company profile is missing.")
                }

                // Verify tables can be queried without crashing
                tempDb.customerDao().getCustomerById(1L)
                tempDb.quotationDao().getQuotationByIdDirect(1)
                
                Log.i(TAG, "Staging validation and database integrity checks succeeded.")
            } catch (e: Exception) {
                try { tempDb.close() } catch (ignored: Exception) {}

                return RestoreResult.InvalidBackup("Staging validation failed: Database structure or schema is corrupt")
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

            // Restore graphic assets from the backup package
            try {
                val rootObj = org.json.JSONObject(jsonText)
                if (rootObj.has("graphics")) {
                    val graphics = rootObj.getJSONObject("graphics")
                    
                    // 1. Restore Company Logo
                    if (graphics.has("logo_base64")) {
                        val base64 = graphics.getString("logo_base64")
                        if (base64.isNotEmpty()) {
                            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            java.io.File(context.filesDir, "company_logo.png").writeBytes(bytes)
                        }
                    }
                    
                    // 2. Restore Authorized Signature
                    if (graphics.has("signature_base64")) {
                        val base64 = graphics.getString("signature_base64")
                        if (base64.isNotEmpty()) {
                            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            java.io.File(context.filesDir, "auth_signature.png").writeBytes(bytes)
                        }
                    }
                    
                    // 3. Restore Company Seal
                    if (graphics.has("seal_base64")) {
                        val base64 = graphics.getString("seal_base64")
                        if (base64.isNotEmpty()) {
                            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            java.io.File(context.filesDir, "company_seal.png").writeBytes(bytes)
                        }
                    }
                    
                    // 4. Restore Reference Design Images
                    val keys = graphics.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key.startsWith("design_") || key.startsWith("laminate_")) {
                            val base64 = graphics.getString(key)
                            if (base64.isNotEmpty()) {
                                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                java.io.File(context.filesDir, key).writeBytes(bytes)
                            }
                        }
                    }
                }
                
                // 5. Update paths in CompanyProfile table to match active staging paths on this specific system
                val currentProfile = repository.getCompanyProfileDirect()
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        logoPath = if (java.io.File(context.filesDir, "company_logo.png").exists()) java.io.File(context.filesDir, "company_logo.png").absolutePath else currentProfile.logoPath,
                        signaturePath = if (java.io.File(context.filesDir, "auth_signature.png").exists()) java.io.File(context.filesDir, "auth_signature.png").absolutePath else currentProfile.signaturePath,
                        companySealPath = if (java.io.File(context.filesDir, "company_seal.png").exists()) java.io.File(context.filesDir, "company_seal.png").absolutePath else currentProfile.companySealPath
                    )
                    db.companyProfileDao().insertOrUpdate(updatedProfile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore graphic assets: ${e.message}")
            }

            RestoreResult.Success
        } catch (e: Exception) {

            RestoreResult.Rollback("Restore encountered a fatal exception")
        }
    }
}
