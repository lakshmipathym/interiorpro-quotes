package com.example.core.backup.restore

interface RestoreValidationRepository {
    fun getCurrentAppVersion(): String
    fun getCurrentDatabaseVersion(): Int
    fun getSupportedBackupVersions(): List<Int>
    fun getSupportedEncryptionTypes(): List<String>
}
