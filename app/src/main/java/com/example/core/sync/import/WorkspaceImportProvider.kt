package com.example.core.sync.import

import kotlinx.coroutines.flow.Flow

interface WorkspaceImportProvider {
    suspend fun readPackage(filePath: String): WorkspaceImportPackage?
    suspend fun verifyPackageStructure(importPackage: WorkspaceImportPackage): ImportResult
    suspend fun verifyPackageVersion(metadata: ImportMetadata): ImportResult
    suspend fun verifyCompatibility(metadata: ImportMetadata): ImportResult
    suspend fun prepareImportSession(importPackage: WorkspaceImportPackage): String
    fun observeRecentImports(): Flow<List<ImportSummary>>
}
