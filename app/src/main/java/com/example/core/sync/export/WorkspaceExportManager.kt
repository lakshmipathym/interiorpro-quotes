package com.example.core.sync.export

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WorkspaceExportManager(
    private val repository: WorkspaceExportRepository
) : WorkspaceExportProvider {

    override suspend fun createExportPackage(
        deviceName: String,
        appVersion: String,
        databaseVersion: Int,
        isEncrypted: Boolean,
        isCompressed: Boolean
    ): WorkspaceExportPackage {
        val exportId = UUID.randomUUID().toString()
        val metadata = ExportMetadata(
            exportId = exportId,
            date = System.currentTimeMillis(),
            deviceName = deviceName,
            appVersion = appVersion,
            databaseVersion = databaseVersion,
            isEncrypted = isEncrypted,
            isCompressed = isCompressed,
            formatVersion = 1
        )

        val manifest = mapOf(
            "customers" to "included",
            "quotations" to "included",
            "masterData" to "included",
            "companyProfile" to "included",
            "themeSettings" to "included",
            "paymentSettings" to "included",
            "termsAndConditions" to "included",
            "pdfPreferences" to "included",
            "applicationSettings" to "included"
        )
        val assets = listOf("logo.png", "signature.png", "seal.png")
        val settings = mapOf<String, Any>(
            "pdfPreferences" to "default", 
            "paymentSettings" to "default"
        )

        return WorkspaceExportPackage(
            metadata = metadata,
            manifest = manifest,
            databasePath = "/exports/temp/$exportId/database.db",
            assets = assets,
            settings = settings
        )
    }

    override suspend fun finalizeExport(exportPackage: WorkspaceExportPackage): ExportSummary {
        val startTime = System.currentTimeMillis()
        
        return try {
            repository.saveExportPackage(exportPackage)
            
            val summary = ExportSummary(
                exportId = exportPackage.metadata.exportId,
                success = true,
                packageSize = 1024L * 1024L, 
                totalItemsExported = exportPackage.manifest.size + exportPackage.assets.size,
                durationMillis = System.currentTimeMillis() - startTime
            )
            
            repository.saveExportSummary(summary)
            summary
        } catch (e: Exception) {
            val failureSummary = ExportSummary(
                exportId = exportPackage.metadata.exportId,
                success = false,
                packageSize = 0L,
                totalItemsExported = 0,
                durationMillis = System.currentTimeMillis() - startTime,
                errorMessage = "Export failed"
            )
            repository.saveExportSummary(failureSummary)
            failureSummary
        }
    }

    override fun observeRecentExports(): Flow<List<ExportSummary>> {
        return repository.observeRecentExports()
    }
}
