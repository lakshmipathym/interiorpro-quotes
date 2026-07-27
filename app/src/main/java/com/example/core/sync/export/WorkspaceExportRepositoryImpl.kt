package com.example.core.sync.export

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WorkspaceExportRepositoryImpl : WorkspaceExportRepository {
    private val exportPackages = mutableMapOf<String, WorkspaceExportPackage>()
    private val _recentExports = MutableStateFlow<List<ExportSummary>>(emptyList())

    override suspend fun saveExportPackage(exportPackage: WorkspaceExportPackage): String {
        val id = exportPackage.metadata.exportId
        exportPackages[id] = exportPackage
        return id
    }

    override suspend fun getExportPackage(exportId: String): WorkspaceExportPackage? {
        return exportPackages[exportId]
    }

    override fun observeRecentExports(): Flow<List<ExportSummary>> = _recentExports.asStateFlow()

    override suspend fun saveExportSummary(summary: ExportSummary) {
        _recentExports.update { current ->
            (listOf(summary) + current).take(10)
        }
    }
}
