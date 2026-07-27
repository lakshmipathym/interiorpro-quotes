package com.example.core.backup.sandbox

sealed class SandboxValidationResult {
    data class Success(val message: String) : SandboxValidationResult()
    data class ChecksumMismatch(val expected: String, val actual: String) : SandboxValidationResult()
    data class DecryptionFailed(val reason: String) : SandboxValidationResult()
    data class InvalidSchema(val reason: String) : SandboxValidationResult()
    data class ExtractionFailed(val reason: String) : SandboxValidationResult()
    data class UnknownError(val reason: String) : SandboxValidationResult()
}
