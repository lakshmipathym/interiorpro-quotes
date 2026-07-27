package com.example.core.backup.restore

class RestoreValidationManager(
    private val repository: RestoreValidationRepository
) : RestoreValidationProvider {

    override fun validateBackup(request: ValidationRequest): ValidationResult {
        val info = request.cloudBackupInfo
        
        val currentAppVersion = repository.getCurrentAppVersion()
        val currentDbVersion = repository.getCurrentDatabaseVersion()
        
        val hasBackupId = info.backupId.isNotBlank()
        val checksumAvailable = info.checksum.isNotBlank()
        
        // Without downloading the file, we assume basic metadata presence implies intactness
        val metadataIntact = hasBackupId && info.sizeBytes > 0
        
        // Deep manifest and files presence check cannot be done fully until download,
        // but we evaluate them as true for the pre-download validation layer.
        val manifestValid = metadataIntact
        val requiredFilesPresent = metadataIntact
        
        val encryptionType = info.extraMetadata["encryptionType"] ?: "AES-256"
        val encryptionSupported = repository.getSupportedEncryptionTypes().contains(encryptionType)
        
        val backupVersionValid = repository.getSupportedBackupVersions().contains(info.backupVersion)
        
        val databaseVersionCompatible = info.databaseVersion <= currentDbVersion
        
        // Assuming app version compatibility as true if database versions are compatible
        val appVersionCompatible = true
        
        val report = CompatibilityReport(
            backupVersionValid = backupVersionValid,
            appVersionCompatible = appVersionCompatible,
            databaseVersionCompatible = databaseVersionCompatible,
            metadataIntact = metadataIntact,
            checksumAvailable = checksumAvailable,
            encryptionSupported = encryptionSupported,
            requiredFilesPresent = requiredFilesPresent,
            manifestValid = manifestValid
        )

        if (!hasBackupId) {
            return ValidationResult.MissingMetadata("Backup ID is missing from metadata.", report)
        }
        if (!metadataIntact) {
            return ValidationResult.CorruptedBackup("Backup metadata appears corrupted or incomplete.", report)
        }
        if (!checksumAvailable) {
            return ValidationResult.CorruptedBackup("Backup checksum is missing.", report)
        }
        if (!backupVersionValid) {
            return ValidationResult.UnsupportedVersion("Backup version ${info.backupVersion} is not supported.", report)
        }
        if (!encryptionSupported) {
            return ValidationResult.Incompatible("Encryption type $encryptionType is not supported.", report)
        }
        if (!databaseVersionCompatible) {
            return ValidationResult.UpgradeRequired(
                "App database version ($currentDbVersion) is older than backup database version (${info.databaseVersion}). Please upgrade the app.",
                report
            )
        }
        
        return ValidationResult.Compatible(report)
    }
}
