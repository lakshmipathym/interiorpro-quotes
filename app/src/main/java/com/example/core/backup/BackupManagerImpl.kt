package com.example.core.backup

import android.util.Log
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.core.security.EncryptionManager
import com.example.core.security.ChecksumManager
import com.example.core.device.DeviceManager
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

class BackupManagerImpl(
    private val db: AppDatabase,
    private val repository: QuotesRepository,
    private val encryptionManager: EncryptionManager,
    private val checksumManager: ChecksumManager,
    private val deviceManager: DeviceManager
) : BackupManager {

    companion object {
        private const val TAG = "BackupManagerImpl"
    }

    override suspend fun createBackup(
        destinationFile: File,
        password: String,
        encrypt: Boolean,
        compress: Boolean
    ): BackupResult {
        return try {
            Log.i(TAG, "Starting enterprise backup generation pipeline...")

            // 1. Create Backup (serialize database tables to raw encrypted/unencrypted format via existing engine)
            // To ensure zero regression and utilize verified logic, we generate the raw backup JSON.
            // We pass an empty password to get raw JSON text first, which we will compress and encrypt ourselves.
            val rawBackupJson = com.example.backup.BackupManager.exportBackup(db, repository, password = "")
            
            var processedData = rawBackupJson.toByteArray(Charsets.UTF_8)

            // 2. Compress via GZIP
            if (compress) {

                val bos = ByteArrayOutputStream()
                GZIPOutputStream(bos).use { gzip ->
                    gzip.write(processedData)
                }
                processedData = bos.toByteArray()
            }

            // 3. AES-256 Encrypt
            if (encrypt) {

                processedData = encryptionManager.encrypt(processedData, password.ifEmpty { "InteriorProSecureBackupDefault" })
            }

            // Write final backup bytes to the physical destination file
            destinationFile.writeBytes(processedData)

            // 4. Generate SHA-256 checksum of the finished archive
            val sha256Checksum = checksumManager.computeFileSha256(destinationFile)

            // 5. Generate metadata profile
            val metadata = BackupMetadata(
                version = 1,
                timestamp = System.currentTimeMillis(),
                checksum = sha256Checksum,
                databaseVersion = deviceManager.getDatabaseVersion(),
                appVersion = deviceManager.getAppVersion(),
                deviceId = deviceManager.getDeviceId(),
                deviceName = deviceManager.getDeviceName(),
                isEncrypted = encrypt,
                isCompressed = compress,
                recordCount = getRecordCount()
            )

            Log.i(TAG, "Backup pipeline completed successfully. Checksum: $sha256Checksum")
            BackupResult.Success(destinationFile, metadata)
        } catch (e: Exception) {

            BackupResult.Failure("Backup pipeline execution failed")
        }
    }

    private suspend fun getRecordCount(): Int {
        return try {
            val customerCount = repository.allCustomers.first().size
            val quotationCount = repository.allQuotations.first().size
            customerCount + quotationCount
        } catch (e: Exception) {
            0
        }
    }
}
