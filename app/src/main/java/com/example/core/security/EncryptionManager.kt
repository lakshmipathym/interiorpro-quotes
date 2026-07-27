package com.example.core.security

import java.io.File

/**
 * EncryptionManager specifies standard enterprise cryptographic primitives for securing system assets.
 */
interface EncryptionManager {
    /**
     * Configures the active encryption configuration.
     */
    fun setConfig(config: EncryptionConfig)

    /**
     * Retrieves the active encryption configuration.
     */
    fun getConfig(): EncryptionConfig?

    /**
     * Encrypts a byte array using the active encryption configuration.
     */
    fun encrypt(data: ByteArray): EncryptionResult

    /**
     * Decrypts an EncryptionResult using the active encryption configuration.
     */
    fun decrypt(result: EncryptionResult): ByteArray

    /**
     * Encrypts a byte array using AES-256 (standard security with password-based key derivation).
     */
    fun encrypt(data: ByteArray, password: String): ByteArray

    /**
     * Decrypts a byte array using AES-256.
     */
    fun decrypt(encryptedData: ByteArray, password: String): ByteArray

    /**
     * Secures a whole local file and saves the ciphertext to a target location.
     */
    fun encryptFile(inputFile: File, outputFile: File, password: String): Boolean

    /**
     * Decodes a cipher-text file to its original state.
     */
    fun decryptFile(inputFile: File, outputFile: File, password: String): Boolean
}
