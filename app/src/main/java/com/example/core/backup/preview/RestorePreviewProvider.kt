package com.example.core.backup.preview

import com.example.core.backup.pkg.BackupPackage

interface RestorePreviewProvider {
    suspend fun generatePreview(backupPackage: BackupPackage): RestorePreview
}
