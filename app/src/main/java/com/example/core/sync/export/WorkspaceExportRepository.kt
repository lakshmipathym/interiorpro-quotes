package com.example.core.sync.export

import kotlinx.coroutines.flow.Flow

interface WorkspaceExportRepository {
    suspend fun saveExportPackage(exportPackage: WorkspaceExportPackage): String
    suspend fun getExportPackage(exportId: String): WorkspaceExportPackage?
    fun observeRecentExports(): Flow<List<ExportSummary>>
    suspend fun saveExportSummary(summary: ExportSummary)
}
