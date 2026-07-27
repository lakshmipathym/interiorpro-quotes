package com.example.core.security

interface ChecksumProvider {
    val algorithmName: String
    fun calculate(data: ByteArray): ChecksumResult
    fun verify(data: ByteArray, expectedChecksum: ChecksumResult): Boolean
}
