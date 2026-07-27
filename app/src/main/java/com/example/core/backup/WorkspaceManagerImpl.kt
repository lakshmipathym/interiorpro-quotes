package com.example.core.backup

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.core.security.EncryptionManager
import com.example.core.security.ChecksumManager
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class WorkspaceManagerImpl(
    private val context: Context,
    private val db: AppDatabase,
    private val repository: QuotesRepository,
    private val encryptionManager: EncryptionManager,
    private val checksumManager: ChecksumManager
) : WorkspaceManager {

    companion object {
        private const val TAG = "WorkspaceManagerImpl"
        private const val WORKSPACE_PASSWORD = "InteriorProSecureBackupDefault"
    }

    override suspend fun exportWorkspace(destinationFile: File): File {
        try {
            Log.i(TAG, "Starting workspace bundle packaging (.ipro)...")

            // 1. Export database payload to raw json format using current engine
            val rawBackupJson = com.example.backup.BackupManager.exportBackup(db, repository, password = "")
            val rootJson = JSONObject(rawBackupJson)

            // 2. Add Graphic Assets as Base64 inside a dedicated JSON node
            val graphicsObj = JSONObject()

            val logoFile = File(context.filesDir, "company_logo.png")
            if (logoFile.exists()) {
                val bytes = logoFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                graphicsObj.put("logo_base64", base64)
            }

            val signatureFile = File(context.filesDir, "auth_signature.png")
            if (signatureFile.exists()) {
                val bytes = signatureFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                graphicsObj.put("signature_base64", base64)
            }

            val sealFile = File(context.filesDir, "company_seal.png")
            if (sealFile.exists()) {
                val bytes = sealFile.readBytes()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                graphicsObj.put("seal_base64", base64)
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
            rootJson.put("graphics", graphicsObj)

            // 3. Mark as a professional .ipro workspace package
            rootJson.put("workspace_format", "ipro")
            rootJson.put("workspace_version", 1)

            val fullJsonText = rootJson.toString()

            // 4. Generate SHA-256 Checksum of the plain JSON representation
            val checksum = checksumManager.computeSha256(fullJsonText.toByteArray(Charsets.UTF_8))
            rootJson.put("workspace_checksum", checksum)

            // Re-stringify with checksum
            val finalJsonText = rootJson.toString()
            var processedData = finalJsonText.toByteArray(Charsets.UTF_8)

            // 5. Compress using GZIP
            val bos = ByteArrayOutputStream()
            GZIPOutputStream(bos).use { gzip ->
                gzip.write(processedData)
            }
            processedData = bos.toByteArray()

            // 6. Encrypt using AES-256 with standard key pairing
            val encryptedBytes = encryptionManager.encrypt(processedData, WORKSPACE_PASSWORD)

            // 7. Write to the destination file
            destinationFile.writeBytes(encryptedBytes)
            Log.i(TAG, "Workspace package successfully compiled and written to: ${destinationFile.name}")

            return destinationFile
        } catch (e: Exception) {

            throw e
        }
    }

    override suspend fun verifyAndPreviewWorkspace(workspaceBundleFile: File): WorkspacePreview {
        return try {
            if (!workspaceBundleFile.exists() || workspaceBundleFile.length() == 0L) {
                return WorkspacePreview(isValid = false, errorReason = "File is empty or does not exist.")
            }
            if (workspaceBundleFile.length() > 50 * 1024 * 1024) {
                return WorkspacePreview(isValid = false, errorReason = "File exceeds maximum size (50MB).")
            }

            // 1. Read encrypted bytes
            var dataBytes = workspaceBundleFile.readBytes()

            // 2. AES-256 Decrypt
            dataBytes = encryptionManager.decrypt(dataBytes, WORKSPACE_PASSWORD)

            // 3. GZIP Decompress
            val plainJsonText = valGzipDecompress(dataBytes) ?: return WorkspacePreview(isValid = false, errorReason = "Failed to decompress file archive.")

            // 4. Parse JSONObject
            val root = JSONObject(plainJsonText)

            // 5. Verify workspace format
            if (!root.has("workspace_format") || root.getString("workspace_format") != "ipro") {
                return WorkspacePreview(isValid = false, errorReason = "Invalid file format. Not a recognized InteriorPro workspace (.ipro).")
            }

            // Check version
            val version = root.optInt("workspace_version", 1)
            if (version > 1) {
                return WorkspacePreview(isValid = false, errorReason = "Unsupported workspace version: v$version")
            }

            // 6. Extract Preview Information
            var companyName = "Unknown Business"
            if (root.has("company_profile")) {
                companyName = root.getJSONObject("company_profile").optString("companyName", companyName)
            }

            val quotationCount = if (root.has("quotations")) root.getJSONArray("quotations").length() else 0
            val customerCount = if (root.has("customers")) root.getJSONArray("customers").length() else 0

            var hasLogo = false
            var hasSignature = false
            var hasSeal = false

            if (root.has("graphics")) {
                val graphics = root.getJSONObject("graphics")
                hasLogo = graphics.has("logo_base64") && graphics.getString("logo_base64").isNotEmpty()
                hasSignature = graphics.has("signature_base64") && graphics.getString("signature_base64").isNotEmpty()
                hasSeal = graphics.has("seal_base64") && graphics.getString("seal_base64").isNotEmpty()
            }

            WorkspacePreview(
                isValid = true,
                companyName = companyName,
                quotationCount = quotationCount,
                customerCount = customerCount,
                hasLogo = hasLogo,
                hasSignature = hasSignature,
                hasSeal = hasSeal,
                rawJsonText = plainJsonText
            )
        } catch (e: Exception) {

            WorkspacePreview(isValid = false, errorReason = "Cryptographic decryption or signature verification failed. The file may be corrupt or encrypted differently.")
        }
    }

    override suspend fun importWorkspace(rawJsonText: String): Boolean {
        return try {
            Log.i(TAG, "Executing full workspace import and restoration database migration...")

            val root = JSONObject(rawJsonText)

            // 1. Restore standard database records (will transactional clear tables and fill them)
            val dbSuccess = com.example.backup.BackupManager.importBackup(db, repository, rawJsonText, password = "")
            if (!dbSuccess) {

                return false
            }

            // 2. Decode and save graphic files to internal storage
            if (root.has("graphics")) {
                val graphics = root.getJSONObject("graphics")

                if (graphics.has("logo_base64")) {
                    val base64 = graphics.getString("logo_base64")
                    if (base64.isNotEmpty()) {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        File(context.filesDir, "company_logo.png").writeBytes(bytes)
                    }
                }

                if (graphics.has("signature_base64")) {
                    val base64 = graphics.getString("signature_base64")
                    if (base64.isNotEmpty()) {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        File(context.filesDir, "auth_signature.png").writeBytes(bytes)
                    }
                }

                if (graphics.has("seal_base64")) {
                    val base64 = graphics.getString("seal_base64")
                    if (base64.isNotEmpty()) {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        File(context.filesDir, "company_seal.png").writeBytes(bytes)
                    }
                }

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
                // 3. Update paths in CompanyProfile table to match active staging paths on this specific system
                val currentProfile = repository.getCompanyProfileDirect()
                if (currentProfile != null) {
                    val updatedProfile = currentProfile.copy(
                        logoPath = if (File(context.filesDir, "company_logo.png").exists()) File(context.filesDir, "company_logo.png").absolutePath else currentProfile.logoPath,
                        signaturePath = if (File(context.filesDir, "auth_signature.png").exists()) File(context.filesDir, "auth_signature.png").absolutePath else currentProfile.signaturePath,
                        companySealPath = if (File(context.filesDir, "company_seal.png").exists()) File(context.filesDir, "company_seal.png").absolutePath else currentProfile.companySealPath
                    )
                    db.companyProfileDao().insertOrUpdate(updatedProfile)
                }
            }

            Log.i(TAG, "Workspace bundle successfully integrated and fully imported.")
            true
        } catch (e: Exception) {

            false
        }
    }

    override suspend fun restoreWorkspace(workspaceBundleFile: File): Boolean {
        val preview = verifyAndPreviewWorkspace(workspaceBundleFile)
        if (!preview.isValid) return false
        return importWorkspace(preview.rawJsonText)
    }

    private fun valGzipDecompress(bytes: ByteArray): String? {
        return try {
            val bis = ByteArrayInputStream(bytes)
            GZIPInputStream(bis).use { gzip ->
                InputStreamReader(gzip, Charsets.UTF_8).use { reader ->
                    val sb = java.lang.StringBuilder()
                    val buffer = CharArray(8192)
                    var charsRead: Int
                    var totalChars = 0
                    val maxChars = 100 * 1024 * 1024 // 100MB max limit to prevent zip bombs
                    while (reader.read(buffer).also { charsRead = it } != -1) {
                        totalChars += charsRead
                        if (totalChars > maxChars) throw SecurityException("Payload exceeded maximum allowed size")
                        sb.append(buffer, 0, charsRead)
                    }
                    sb.toString()
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
