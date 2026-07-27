package com.example.core.security

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionManagerImpl(
    private val encryptionProvider: EncryptionProvider = AESEncryptionProvider()
) : EncryptionManager {

    companion object {
        private const val ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_ALGORITHM = "AES"
        private const val DIGEST_ALGORITHM = "SHA-256"
    }

    private var activeConfig: EncryptionConfig? = null

    override fun setConfig(config: EncryptionConfig) {
        this.activeConfig = config
    }

    override fun getConfig(): EncryptionConfig? {
        return this.activeConfig
    }

    override fun encrypt(data: ByteArray): EncryptionResult {
        val config = activeConfig ?: throw IllegalStateException("Encryption configuration not set")
        return encryptionProvider.encrypt(data, config)
    }

    override fun decrypt(result: EncryptionResult): ByteArray {
        val config = activeConfig ?: throw IllegalStateException("Encryption configuration not set")
        return encryptionProvider.decrypt(result, config)
    }

    private fun getSecretKeySpec(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        val keyBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    private fun getFixedIv(): IvParameterSpec {
        val ivBytes = ByteArray(16)
        "InteriorProSecure".toByteArray(Charsets.UTF_8).copyInto(ivBytes, 0, 0, minOf(16, "InteriorProSecure".length))
        return IvParameterSpec(ivBytes)
    }

    override fun encrypt(data: ByteArray, password: String): ByteArray {
        val keySpec = getSecretKeySpec(password)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, getFixedIv())
        return cipher.doFinal(data)
    }

    override fun decrypt(encryptedData: ByteArray, password: String): ByteArray {
        val keySpec = getSecretKeySpec(password)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, getFixedIv())
        return cipher.doFinal(encryptedData)
    }

    override fun encryptFile(inputFile: File, outputFile: File, password: String): Boolean {
        return try {
            if (!inputFile.exists()) return false
            val fileBytes = inputFile.readBytes()
            val encryptedBytes = encrypt(fileBytes, password)
            outputFile.writeBytes(encryptedBytes)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun decryptFile(inputFile: File, outputFile: File, password: String): Boolean {
        return try {
            if (!inputFile.exists()) return false
            val encryptedBytes = inputFile.readBytes()
            val decryptedBytes = decrypt(encryptedBytes, password)
            outputFile.writeBytes(decryptedBytes)
            true
        } catch (e: Exception) {
            false
        }
    }
}
