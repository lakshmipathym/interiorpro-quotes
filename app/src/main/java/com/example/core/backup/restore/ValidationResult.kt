package com.example.core.backup.restore

sealed class ValidationResult {
    data class Compatible(val report: CompatibilityReport) : ValidationResult()
    data class Incompatible(val reason: String, val report: CompatibilityReport) : ValidationResult()
    data class UpgradeRequired(val reason: String, val report: CompatibilityReport) : ValidationResult()
    data class CorruptedBackup(val reason: String, val report: CompatibilityReport) : ValidationResult()
    data class MissingMetadata(val reason: String, val report: CompatibilityReport) : ValidationResult()
    data class UnsupportedVersion(val reason: String, val report: CompatibilityReport) : ValidationResult()
}
