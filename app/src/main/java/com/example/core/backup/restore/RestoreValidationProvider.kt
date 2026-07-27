package com.example.core.backup.restore

interface RestoreValidationProvider {
    fun validateBackup(request: ValidationRequest): ValidationResult
}
