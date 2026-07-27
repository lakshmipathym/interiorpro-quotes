package com.example.core.sync.export

import kotlinx.coroutines.flow.Flow

interface WorkspaceExportProvider {
    suspend fun createExportPackage(
        deviceName: String,
        appVersion: String,
        databaseVersion: Int,
        isEncrypted: Boolean,
        isCompressed: Boolean
    ): WorkspaceExportPackage

    suspend fun finalizeExport(exportPackage: WorkspaceExportPackage): ExportSummary
    fun observeRecentExports(): Flow<List<ExportSummary>>
}
