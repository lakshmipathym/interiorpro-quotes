package com.example.core.backup

/**
 * BackupMetadata keeps track of crucial system information, checksum validation, and security states
 * to protect enterprise restore operations.
 */
data class BackupMetadata(
    val version: Int,
    val timestamp: Long,
    val checksum: String,
    val databaseVersion: Int,
    val appVersion: String,
    val deviceId: String,
    val deviceName: String,
    val isEncrypted: Boolean,
    val isCompressed: Boolean,
    val recordCount: Int,
    // Future-Ready Security Architecture
    val hasDigitalSignature: Boolean = false,
    val digitalSignature: String? = null,
    val requiresTrustedDevice: Boolean = false,
    val trustedDeviceIds: List<String> = emptyList(),
    val requiresEnterpriseLicense: Boolean = false,
    val enterpriseLicenseId: String? = null,
    val extraMetadata: Map<String, String> = emptyMap()
)
