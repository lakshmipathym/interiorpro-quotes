package com.example.core.backup

import java.io.File

/**
 * RestoreResult represents the outcome of the restore workflow.
 */
sealed interface RestoreResult {
    object Success : RestoreResult
    data class InvalidBackup(val reason: String) : RestoreResult
    data class Rollback(val reason: String, val throwable: Throwable? = null) : RestoreResult
}

/**
 * RestoreManager safely loads backup files, validates their contents in a sandboxed staging phase,
 * and performs atomic transaction-based restores with rolling back capability.
 */
interface RestoreManager {
    /**
     * Verifies basic file authenticity, format matching, and checksum validity.
     */
    suspend fun verifyBackupIntegrity(backupFile: File, password: String = ""): Boolean

    /**
     * Performs a staging-first restore process:
     * 1. Decrypts and unpacks backup to a temporary staging folder.
     * 2. Runs semantic, structural, and schema checks on staging databases.
     * 3. Performs transaction-safe migration or overwrite, rolling back instantly upon failure.
     * Never overwrites the main database directly without staging verification.
     */
    suspend fun safeRestore(
        backupFile: File,
        password: String = ""
    ): RestoreResult
}
