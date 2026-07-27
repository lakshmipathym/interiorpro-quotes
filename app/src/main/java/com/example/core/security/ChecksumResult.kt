package com.example.core.security

data class ChecksumResult(
    val checksum: String,
    val algorithm: String = "SHA-256"
)
