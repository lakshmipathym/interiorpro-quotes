package com.example.core.backup.restore.execution

import com.example.core.backup.pkg.BackupPackage
import com.example.core.backup.preview.RestorePreview
import com.example.core.backup.restore.ValidationResult

data class RestoreExecutionRequest(
    val backupPackage: BackupPackage,
    val passwordUsed: String,
    val preview: RestorePreview,
    val validationResult: ValidationResult
)
