package com.example.core.backup

import java.io.File

/**
 * BackupResult represents the output of the backup operation.
 */
sealed interface BackupResult {
    data class Success(val backupFile: File, val metadata: BackupMetadata) : BackupResult
    data class Failure(val reason: String) : BackupResult
}

/**
 * BackupManager handles the creation of system and workspace backups.
 */
interface BackupManager {
    /**
     * Combines database state, files, and metadata into a secure, encrypted backup package.
     */
    suspend fun createBackup(
        destinationFile: File,
        password: String = "",
        encrypt: Boolean = true,
        compress: Boolean = true
    ): BackupResult
}
