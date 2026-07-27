package com.example.core.backup.restore

import com.example.core.backup.discovery.CloudBackupInfo

data class ValidationRequest(
    val cloudBackupInfo: CloudBackupInfo
)
