package com.example.core.sync.validation

data class IntegrityReport(
    val resultType: ValidationResultType,
    val isChecksumValid: Boolean,
    val isEncryptionValid: Boolean,
    val errors: List<ValidationError>
)
