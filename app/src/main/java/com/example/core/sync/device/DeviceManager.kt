package com.example.core.sync.device

import android.util.Log

class DeviceManager(
    private val repository: DeviceRepository
) : DeviceProvider {

    companion object {
        private const val TAG = "DeviceManager"
    }

    override suspend fun getCurrentDevice(): CurrentDevice? {
        val localId = repository.getLocalDeviceId() ?: return null
        val info = repository.getDevice(localId) ?: return null
        return CurrentDevice(info)
    }

    override suspend fun getLinkedDevices(): List<LinkedDevice> {
        val localId = repository.getLocalDeviceId()
        return repository.getAllDevices()
            .filter { it.deviceId != localId }
            .map { LinkedDevice(it, isTrusted = true) }
    }

    override suspend fun updateLastSeen(deviceId: String, timestamp: Long) {
        repository.getDevice(deviceId)?.let {
            val updated = it.copy(lastSeen = timestamp, status = DeviceStatus.ONLINE)
            repository.saveDevice(updated)

        }
    }

    override suspend fun updateBackupStatus(deviceId: String, backupTime: Long, size: Long) {
        repository.getDevice(deviceId)?.let {
            val updated = it.copy(lastBackup = backupTime, backupSize = size)
            repository.saveDevice(updated)

        }
    }

    override suspend fun updateSyncStatus(deviceId: String, syncTime: Long) {
        repository.getDevice(deviceId)?.let {
            val updated = it.copy(lastSync = syncTime)
            repository.saveDevice(updated)

        }
    }
}
