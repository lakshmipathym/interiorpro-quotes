package com.example.core.backup.preview

import com.example.core.backup.pkg.BackupPackage
import com.example.core.device.DeviceManager

class RestorePreviewRepositoryImpl(
    private val deviceManager: DeviceManager
) : RestorePreviewRepository {

    override fun extractWorkspaceSummary(backupPackage: BackupPackage): RestoreSummary {
        val summary = backupPackage.manifest.contentSummary
        val fileList = backupPackage.manifest.fileList
        
        return RestoreSummary(
            totalCustomers = summary["customers"] ?: 0,
            totalQuotations = summary["quotations"] ?: 0,
            totalMasters = (summary["masters_entities"] ?: 0),
            companyProfileAvailable = summary.containsKey("companyProfile") || backupPackage.manifest.metadata.extraMetadata.containsKey("companyProfile") || backupPackage.manifest.metadata.extraMetadata.containsKey("theme"),
            logoAvailable = fileList.contains("company_logo.png"),
            signatureAvailable = fileList.contains("auth_signature.png"),
            companySealAvailable = fileList.contains("company_seal.png")
        )
    }

    override fun calculateImpactSummary(
        backupPackage: BackupPackage,
        workspaceSummary: RestoreSummary
    ): RestoreImpact {
        val totalRecords = workspaceSummary.totalCustomers + workspaceSummary.totalQuotations + workspaceSummary.totalMasters
        
        val backupDbVersion = backupPackage.manifest.metadata.databaseVersion
        val currentDbVersion = deviceManager.getDatabaseVersion()
        
        val compatibilityStatus = if (backupDbVersion <= currentDbVersion) {
            "Compatible"
        } else {
            "Incompatible - Upgrade Required"
        }

        val difference = RestoreDifference(
            recordsToAdd = totalRecords,
            recordsToUpdate = 0,
            recordsToReplace = 0,
            potentialConflicts = 0
        )

        return RestoreImpact(
            totalDifference = difference,
            compatibilityStatus = compatibilityStatus
        )
    }
}
