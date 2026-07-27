package com.example.core.sync.validation

interface PackageIntegrityProvider {
    suspend fun verifyChecksum(filePath: String, expectedChecksum: String): IntegrityReport
    suspend fun verifyEncryption(filePath: String, isEncrypted: Boolean): IntegrityReport
    
    // Architecture preparation for future security features
    suspend fun verifyDigitalSignature(filePath: String): IntegrityReport
    suspend fun verifyCertificate(filePath: String): IntegrityReport
    suspend fun verifyTrustedPackage(filePath: String): IntegrityReport
    suspend fun verifyEnterpriseLicense(filePath: String): IntegrityReport
}
