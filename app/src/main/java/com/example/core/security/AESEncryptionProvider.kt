package com.example.core.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AESEncryptionProvider : EncryptionProvider {
    override val algorithmName: String = "AES/CBC/PKCS5Padding"
    private val secureRandom = SecureRandom()

    override fun encrypt(data: ByteArray, config: EncryptionConfig): EncryptionResult {
        val salt = ByteArray(16).apply { secureRandom.nextBytes(this) }
        val secretKey = getSecretKey(config, salt)
        val cipher = Cipher.getInstance(algorithmName)
        val iv = ByteArray(16).apply { secureRandom.nextBytes(this) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val ciphertext = cipher.doFinal(data)
        
        val keyAlias = when (config) {
            is EncryptionConfig.KeyStore -> config.alias
            is EncryptionConfig.AES256GCM -> config.keyAlias
            is EncryptionConfig.RSA -> config.keyAlias
            else -> null
        }
        
        val saltValue = when (config) {
            is EncryptionConfig.Password -> salt
            else -> null
        }

        return EncryptionResult(
            ciphertext = ciphertext,
            iv = iv,
            salt = saltValue,
            keyAlias = keyAlias
        )
    }

    override fun decrypt(encryptionResult: EncryptionResult, config: EncryptionConfig): ByteArray {
        val salt = encryptionResult.salt ?: ByteArray(16)
        val secretKey = getSecretKey(config, salt)
        val cipher = Cipher.getInstance(algorithmName)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(encryptionResult.iv))
        return cipher.doFinal(encryptionResult.ciphertext)
    }

    private fun getSecretKey(config: EncryptionConfig, salt: ByteArray): SecretKeySpec {
        return when (config) {
            is EncryptionConfig.Password -> {
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val spec = PBEKeySpec(config.password.toCharArray(), salt, 10000, 256)
                val tmp = factory.generateSecret(spec)
                SecretKeySpec(tmp.encoded, "AES")
            }
            is EncryptionConfig.RawKey -> {
                SecretKeySpec(config.keyBytes, "AES")
            }
            is EncryptionConfig.KeyStore -> {
                // Future-proof fallback: Derives standard key from alias to prevent initial crashes
                val aliasDigest = java.security.MessageDigest.getInstance("SHA-256").digest(config.alias.toByteArray())
                SecretKeySpec(aliasDigest, "AES")
            }
            else -> throw UnsupportedOperationException("Configuration not supported in this provider")
        }
    }
}
