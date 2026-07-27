package com.example.core.backup.sandbox

import android.content.Context
import com.example.core.security.EncryptionManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.GZIPInputStream

class RestoreSandboxRepositoryImpl(
    private val context: Context,
    private val encryptionManager: EncryptionManager
) : RestoreSandboxRepository {

    override fun createSandboxWorkspace(): SandboxSession {
        val sessionId = UUID.randomUUID().toString()
        val workspaceDir = File(context.cacheDir, "sandbox_$sessionId")
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs()
        }
        return SandboxSession(sessionId, workspaceDir, System.currentTimeMillis())
    }

    override fun extractAndDecrypt(
        session: SandboxSession,
        encryptedPayload: ByteArray,
        password: String
    ): ByteArray {
        val compressedBytes = encryptionManager.decrypt(encryptedPayload, password)
        val bos = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(compressedBytes)).use { gzip ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytes = 0
            val maxBytes = 100 * 1024 * 1024 // 100MB max limit to prevent zip bombs
            while (gzip.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
                if (totalBytes > maxBytes) throw SecurityException("Payload exceeded maximum allowed size")
                bos.write(buffer, 0, bytesRead)
            }
        }
        return bos.toByteArray()
    }

    override fun destroySandbox(session: SandboxSession) {
        if (session.workspaceDir.exists()) {
            session.workspaceDir.deleteRecursively()
        }
    }

    override fun verifySchema(decryptedPayload: ByteArray): Boolean {
        return try {
            val jsonString = String(decryptedPayload, Charsets.UTF_8)
            jsonString.isNotEmpty() && jsonString.firstOrNull { !it.isWhitespace() } == '{'
        } catch (e: Exception) {
            false
        }
    }
}
