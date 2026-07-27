package com.example.core.sync.import

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WorkspaceImportRepositoryImpl : WorkspaceImportRepository {
    private val cachedPackages = mutableMapOf<String, WorkspaceImportPackage>()
    private val _recentImports = MutableStateFlow<List<ImportSummary>>(emptyList())

    override suspend fun saveImportSummary(summary: ImportSummary) {
        _recentImports.update { current ->
            (listOf(summary) + current).take(10)
        }
    }

    override fun observeRecentImports(): Flow<List<ImportSummary>> = _recentImports.asStateFlow()

    override suspend fun cacheImportPackage(importPackage: WorkspaceImportPackage): String {
        val id = importPackage.metadata.exportId
        cachedPackages[id] = importPackage
        return id
    }

    override suspend fun getCachedPackage(importId: String): WorkspaceImportPackage? {
        return cachedPackages[importId]
    }
}
