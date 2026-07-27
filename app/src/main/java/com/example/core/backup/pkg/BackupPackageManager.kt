package com.example.core.backup.pkg

import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.core.security.EncryptionManager
import com.example.core.security.ChecksumManager
import com.example.core.device.DeviceManager
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.flow.first

class BackupPackageManager(
    private val context: Context,
    private val db: AppDatabase,
    private val repository: QuotesRepository,
    private val encryptionManager: EncryptionManager,
    private val checksumManager: ChecksumManager,
    private val deviceManager: DeviceManager,
    private val compressionManager: CompressionManager = CompressionManager(),
    private val metadataGenerator: MetadataGenerator = MetadataGenerator(context, deviceManager)
) : BackupPackageProvider {

    companion object {
        private const val TAG = "BackupPackageManager"
    }

    override suspend fun createBackupPackage(password: String): BackupPackage {
        Log.i(TAG, "Creating standard enterprise BackupPackage...")

        // 1. Collect workspace data dynamically
        val root = JSONObject()

        // Get database counts for contentSummary
        val customers = repository.allCustomers.first()
        val mastersList = db.masterDao().getAllMastersDirect()
        val templates = repository.allTemplates.first()
        val quotations = repository.allQuotations.first()

        val contentSummary = mapOf(
            "customers" to customers.size,
            "masters_entities" to mastersList.size,
            "quotation_templates" to templates.size,
            "quotations" to quotations.size
        )

        // Compile database payloads
        val backupJsonStr = com.example.backup.BackupManager.exportBackup(db, repository, password = "")
        val cleanDecryptedJson = try {
            val isPlaintext = backupJsonStr.isNotEmpty() && backupJsonStr.firstOrNull { !it.isWhitespace() } == '{'
            if (isPlaintext) {
                backupJsonStr
            } else {
                // Decrypt AES with default password which is ""
                val method = com.example.backup.BackupManager::class.java.getDeclaredMethod("decryptAES", String::class.java, String::class.java)
                method.isAccessible = true
                method.invoke(null, backupJsonStr, "") as String
            }
        } catch (e: Exception) {

            "{}"
        }

        val dbJson = JSONObject(cleanDecryptedJson)
        
        // Populate root with db contents
        val keys = dbJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            root.put(key, dbJson.get(key))
        }

        // Add graphics assets dynamically
        val graphicsObj = JSONObject()
        val logoFile = File(context.filesDir, "company_logo.png")
        if (logoFile.exists()) {
            val bytes = logoFile.readBytes()
            graphicsObj.put("logo_base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        val signatureFile = File(context.filesDir, "auth_signature.png")
        if (signatureFile.exists()) {
            val bytes = signatureFile.readBytes()
            graphicsObj.put("signature_base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        val sealFile = File(context.filesDir, "company_seal.png")
        if (sealFile.exists()) {
            val bytes = sealFile.readBytes()
            graphicsObj.put("seal_base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        val filesDir = context.filesDir
        val designFiles = filesDir.listFiles { _, name -> name.startsWith("design_") || name.startsWith("laminate_") }
        designFiles?.forEach { file ->
            if (file.exists()) {
                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                graphicsObj.put(file.name, base64)
            }
        }
        root.put("graphics", graphicsObj)

        // Dynamic properties: theme, payment settings, terms & conditions
        val companyProfile = repository.getCompanyProfileDirect()
        val theme = companyProfile?.tagline ?: "DEFAULT"
        val paymentSettings = companyProfile?.defaultPaymentTerms ?: "DEFAULT_PAYMENT"
        val termsAndConditions = companyProfile?.termsAndConditions ?: "DEFAULT_TERMS"

        root.put("theme", theme)
        root.put("payment_settings", paymentSettings)
        root.put("terms_and_conditions", termsAndConditions)

        // Stringify full workspace JSON
        val fullJsonText = root.toString()
        val plainBytes = fullJsonText.toByteArray(Charsets.UTF_8)

        // 2. Compress data
        val compressedBytes = compressionManager.compress(plainBytes)

        // 3. Generate metadata
        val appVersion = deviceManager.getAppVersion()
        val dbVersion = deviceManager.getDatabaseVersion()
        
        val extra = mapOf(
            "deviceId" to deviceManager.getDeviceId(),
            "androidVersion" to deviceManager.getAndroidVersion(),
            "registrationTime" to deviceManager.getRegistrationTime().toString()
        )

        val metadata = metadataGenerator.generateMetadata(
            backupVersion = 1,
            databaseVersion = dbVersion,
            appVersion = appVersion,
            compressionType = "GZIP",
            encryptionType = "AES-256",
            checksumType = "SHA-256",
            extra = extra
        )

        // 4. Generate manifest
        val fileList = mutableListOf<String>()
        if (logoFile.exists()) fileList.add("company_logo.png")
        if (signatureFile.exists()) fileList.add("auth_signature.png")
        if (sealFile.exists()) fileList.add("company_seal.png")
        
        val systemProperties = mapOf(
            "os" to "Android",
            "model" to deviceManager.getDeviceModel(),
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND
        )

        val manifest = BackupManifest(
            metadata = metadata,
            fileList = fileList,
            contentSummary = contentSummary,
            systemProperties = systemProperties
        )

        // 5. Pass compressed data to EncryptionManager
        val encryptedBytes = encryptionManager.encrypt(compressedBytes, password)

        // 6. Generate checksum of the encrypted payload
        val checksum = checksumManager.computeSha256(encryptedBytes)

        // Return BackupPackage object
        return BackupPackage(
            manifest = manifest,
            encryptedPayload = encryptedBytes,
            checksum = checksum
        )
    }
}
