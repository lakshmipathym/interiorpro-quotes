package com.example.core.security

import java.io.File

/**
 * ChecksumManager computes cryptographic integrity signatures (e.g., SHA-256) for buffers and filesystem units.
 */
interface ChecksumManager {
    /**
     * Calculates the SHA-256 signature of byte arrays.
     */
    fun computeSha256(data: ByteArray): String

    /**
     * Calculates the SHA-256 signature of a physical file.
     */
    fun computeFileSha256(file: File): String

    /**
     * Checks if file's checksum corresponds to an expected signature value.
     */
    fun verifyChecksum(file: File, expectedChecksum: String): Boolean
}
