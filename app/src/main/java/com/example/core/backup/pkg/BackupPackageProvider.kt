package com.example.core.backup.pkg

interface BackupPackageProvider {
    suspend fun createBackupPackage(password: String): BackupPackage
}
