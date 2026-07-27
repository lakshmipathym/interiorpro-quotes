package com.example.core.sync.device

interface DeviceProvider {
    suspend fun getCurrentDevice(): CurrentDevice?
    suspend fun getLinkedDevices(): List<LinkedDevice>
    suspend fun updateLastSeen(deviceId: String, timestamp: Long)
    suspend fun updateBackupStatus(deviceId: String, backupTime: Long, size: Long)
    suspend fun updateSyncStatus(deviceId: String, syncTime: Long)
}
