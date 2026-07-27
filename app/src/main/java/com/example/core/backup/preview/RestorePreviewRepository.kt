package com.example.core.backup.preview

import com.example.core.backup.pkg.BackupPackage

interface RestorePreviewRepository {
    fun extractWorkspaceSummary(backupPackage: BackupPackage): RestoreSummary
    fun calculateImpactSummary(backupPackage: BackupPackage, workspaceSummary: RestoreSummary): RestoreImpact
}
