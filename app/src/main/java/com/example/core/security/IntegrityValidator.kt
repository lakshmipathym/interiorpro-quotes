package com.example.core.security

import com.example.core.backup.BackupMetadata
import java.io.File

/**
 * IntegrityValidator handles validation of complete backup structures, matching schemas, and versions.
 */
interface IntegrityValidator {
    /**
     * Validates if a file's format and structural content conform strictly to stored backup metadata profiles.
     */
    fun validateIntegrity(
        backupFile: File,
        expectedChecksum: String,
        metadata: BackupMetadata
    ): Boolean

    /**
     * Confirms if the backup's schema version aligns safely with the host's current active database version.
     */
    fun isDatabaseVersionCompatible(backupDbVersion: Int, currentDbVersion: Int): Boolean
}
