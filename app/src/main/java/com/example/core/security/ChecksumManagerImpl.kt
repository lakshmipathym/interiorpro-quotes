package com.example.core.security

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class ChecksumManagerImpl : ChecksumManager {

    companion object {
        private const val DIGEST_ALGORITHM = "SHA-256"
    }

    override fun computeSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override fun computeFileSha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        val buffer = ByteArray(8192)
        FileInputStream(file).use { fis ->
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    override fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        val computed = computeFileSha256(file)
        return computed.equals(expectedChecksum, ignoreCase = true)
    }
}
