package com.example.core.sync.import

import kotlinx.coroutines.flow.Flow

interface WorkspaceImportRepository {
    suspend fun saveImportSummary(summary: ImportSummary)
    fun observeRecentImports(): Flow<List<ImportSummary>>
    suspend fun cacheImportPackage(importPackage: WorkspaceImportPackage): String
    suspend fun getCachedPackage(importId: String): WorkspaceImportPackage?
}
