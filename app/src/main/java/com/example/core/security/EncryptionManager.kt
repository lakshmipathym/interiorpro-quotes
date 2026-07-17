package com.example.core.security

import java.io.File

/**
 * EncryptionManager specifies standard enterprise cryptographic primitives for securing system assets.
 */
interface EncryptionManager {
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
