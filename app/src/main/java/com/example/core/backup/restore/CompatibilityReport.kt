package com.example.core.backup.restore

data class CompatibilityReport(
    val backupVersionValid: Boolean,
    val appVersionCompatible: Boolean,
    val databaseVersionCompatible: Boolean,
    val metadataIntact: Boolean,
    val checksumAvailable: Boolean,
    val encryptionSupported: Boolean,
    val requiredFilesPresent: Boolean,
    val manifestValid: Boolean
)
