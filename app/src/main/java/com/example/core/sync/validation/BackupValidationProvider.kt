package com.example.core.sync.validation

interface BackupValidationProvider {
    suspend fun validateStructure(filePath: String): ValidationReport
    suspend fun validateCompatibility(
        appVersion: String,
        databaseVersion: Int,
        packageAppVersion: String,
        packageDatabaseVersion: Int,
        packageFormatVersion: Int
    ): ValidationReport
}
