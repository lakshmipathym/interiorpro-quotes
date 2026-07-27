package com.example.core.sync.import

import kotlinx.coroutines.flow.Flow

class WorkspaceImportManager(
    private val repository: WorkspaceImportRepository
) : WorkspaceImportProvider {

    override suspend fun readPackage(filePath: String): WorkspaceImportPackage? {
        return WorkspaceImportPackage(
            metadata = ImportMetadata(
                exportId = "mock-id",
                date = System.currentTimeMillis(),
                deviceName = "Mock Device",
                appVersion = "1.5.0",
                databaseVersion = 1,
                isEncrypted = false,
                isCompressed = true,
                formatVersion = 1
            ),
            manifest = mapOf(
                "customers" to "included",
                "themeSettings" to "included"
            ),
            databasePath = "mock/database.db",
            assets = listOf("logo.png", "signature.png", "seal.png"),
            settings = emptyMap()
        )
    }

    override suspend fun verifyPackageStructure(importPackage: WorkspaceImportPackage): ImportResult {
        val missingFiles = mutableListOf<String>()
        
        if (importPackage.manifest.isEmpty()) missingFiles.add("manifest")
        if (importPackage.databasePath.isNullOrEmpty()) missingFiles.add("database")
        if (importPackage.assets.isEmpty()) missingFiles.add("assets")
        
        if (!importPackage.assets.contains("logo.png")) missingFiles.add("logo")
        if (!importPackage.assets.contains("signature.png")) missingFiles.add("signature")
        if (!importPackage.assets.contains("seal.png")) missingFiles.add("seal")
        
        if (!importPackage.manifest.containsKey("themeSettings")) missingFiles.add("theme")

        val isValid = missingFiles.isEmpty()

        return ImportResult(
            isSuccessful = isValid,
            isValid = isValid,
            compatibilityIssues = emptyList(),
            missingFiles = missingFiles,
            errorMessage = if (isValid) null else "Package structure is incomplete"
        )
    }

    override suspend fun verifyPackageVersion(metadata: ImportMetadata): ImportResult {
        val isCompatible = metadata.formatVersion <= 1
        return ImportResult(
            isSuccessful = isCompatible,
            isValid = isCompatible,
            compatibilityIssues = if (isCompatible) emptyList() else listOf("Unsupported format version"),
            missingFiles = emptyList(),
            errorMessage = if (isCompatible) null else "Package version is too new"
        )
    }

    override suspend fun verifyCompatibility(metadata: ImportMetadata): ImportResult {
        val issues = mutableListOf<String>()
        if (metadata.databaseVersion > 1) {
            issues.add("Database version from package is newer than current app database")
        }
        
        val isValid = issues.isEmpty()
        return ImportResult(
            isSuccessful = isValid,
            isValid = isValid,
            compatibilityIssues = issues,
            missingFiles = emptyList(),
            errorMessage = if (isValid) null else "Compatibility issues found"
        )
    }

    override suspend fun prepareImportSession(importPackage: WorkspaceImportPackage): String {
        return repository.cacheImportPackage(importPackage)
    }

    override fun observeRecentImports(): Flow<List<ImportSummary>> {
        return repository.observeRecentImports()
    }
}
