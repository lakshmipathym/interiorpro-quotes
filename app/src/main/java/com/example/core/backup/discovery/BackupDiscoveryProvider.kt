package com.example.core.backup.discovery

interface BackupDiscoveryProvider {
    suspend fun discoverBackups(): CloudBackupList
}
