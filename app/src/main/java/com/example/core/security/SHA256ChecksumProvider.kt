package com.example.core.security

import java.security.MessageDigest

class SHA256ChecksumProvider : ChecksumProvider {
    override val algorithmName: String = "SHA-256"

    override fun calculate(data: ByteArray): ChecksumResult {
        val digest = MessageDigest.getInstance(algorithmName)
        val hashBytes = digest.digest(data)
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        return ChecksumResult(hexString, algorithmName)
    }

    override fun verify(data: ByteArray, expectedChecksum: ChecksumResult): Boolean {
        val calculated = calculate(data)
        return calculated.checksum.equals(expectedChecksum.checksum, ignoreCase = true)
    }
}
