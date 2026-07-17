package com.example.core.security

import com.example.core.backup.BackupMetadata
import java.io.File

class IntegrityValidatorImpl(
    private val checksumManager: ChecksumManager
) : IntegrityValidator {

    override fun validateIntegrity(
        backupFile: File,
        expectedChecksum: String,
        metadata: BackupMetadata
    ): Boolean {
        // Step 1: verify file exists
        if (!backupFile.exists()) return false

        // Step 2: verify checksum of the file
        if (!checksumManager.verifyChecksum(backupFile, expectedChecksum)) return false

        // Step 3: verify metadata contains valid structural version matching
        if (metadata.version <= 0 || metadata.databaseVersion <= 0) return false

        return true
    }

    override fun isDatabaseVersionCompatible(backupDbVersion: Int, currentDbVersion: Int): Boolean {
        // Simple logic: Restoring older backups is fully supported.
        // Restoring a backup with a future schema might cause crashes or require forward-migrating.
        return backupDbVersion <= currentDbVersion
    }
}
