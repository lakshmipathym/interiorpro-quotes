package com.example.core.sync.import

data class ImportResult(
    val isSuccessful: Boolean,
    val isValid: Boolean,
    val compatibilityIssues: List<String>,
    val missingFiles: List<String>,
    val errorMessage: String? = null
)
