package com.example.core.backup.preview

import android.util.Log
import com.example.core.backup.pkg.BackupPackage

class RestorePreviewManager(
    private val repository: RestorePreviewRepository
) : RestorePreviewProvider {
    
    companion object {
        private const val TAG = "RestorePreviewManager"
    }

    override suspend fun generatePreview(backupPackage: BackupPackage): RestorePreview {
        Log.i(TAG, "Generating restore preview for confirmation...")
        
        val metadata = backupPackage.manifest.metadata
        val workspaceSummary = repository.extractWorkspaceSummary(backupPackage)
        val impactSummary = repository.calculateImpactSummary(backupPackage, workspaceSummary)
        
        return RestorePreview(
            backupDate = metadata.createdDate,
            backupTime = metadata.createdDate,
            deviceName = metadata.deviceName,
            appVersion = metadata.appVersion,
            databaseVersion = metadata.databaseVersion,
            backupSize = backupPackage.encryptedPayload.size.toLong(),
            workspaceSummary = workspaceSummary,
            impactSummary = impactSummary
        )
    }
}
