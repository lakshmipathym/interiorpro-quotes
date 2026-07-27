package com.example.core.backup.sandbox

import com.example.core.backup.pkg.BackupPackage

interface RestoreSandboxProvider {
    suspend fun validateBackupInSandbox(
        backupPackage: BackupPackage,
        password: String
    ): SandboxValidationResult
}
