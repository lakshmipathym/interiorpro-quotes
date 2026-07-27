package com.example.core.security

sealed class EncryptionConfig {
    data class Password(val password: String, val salt: ByteArray? = null) : EncryptionConfig()
    data class KeyStore(val alias: String) : EncryptionConfig()
    data class RawKey(val keyBytes: ByteArray) : EncryptionConfig()
    // Future-Ready Security Architecture
    data class AES256GCM(val keyAlias: String, val useStrongBox: Boolean = true) : EncryptionConfig()
    data class RSA(val keyAlias: String) : EncryptionConfig()
}
