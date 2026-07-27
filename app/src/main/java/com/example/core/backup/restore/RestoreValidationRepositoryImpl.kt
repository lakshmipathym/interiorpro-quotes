package com.example.core.backup.restore

import com.example.core.device.DeviceManager

class RestoreValidationRepositoryImpl(
    private val deviceManager: DeviceManager
) : RestoreValidationRepository {
    
    override fun getCurrentAppVersion(): String = deviceManager.getAppVersion()
    
    override fun getCurrentDatabaseVersion(): Int = deviceManager.getDatabaseVersion()
    
    override fun getSupportedBackupVersions(): List<Int> = listOf(1)
    
    override fun getSupportedEncryptionTypes(): List<String> = listOf("AES-256", "AES")
}
