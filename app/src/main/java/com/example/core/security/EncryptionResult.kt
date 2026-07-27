package com.example.core.security

data class EncryptionResult(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val salt: ByteArray? = null,
    val keyAlias: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptionResult
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (salt != null) {
            if (other.salt == null) return false
            if (!salt.contentEquals(other.salt)) return false
        } else if (other.salt != null) return false
        if (keyAlias != other.keyAlias) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + (salt?.contentHashCode() ?: 0)
        result = 31 * result + (keyAlias?.hashCode() ?: 0)
        return result
    }
}
