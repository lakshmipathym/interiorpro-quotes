package com.example.core.security

interface EncryptionProvider {
    val algorithmName: String
    fun encrypt(data: ByteArray, config: EncryptionConfig): EncryptionResult
    fun decrypt(encryptionResult: EncryptionResult, config: EncryptionConfig): ByteArray
}
