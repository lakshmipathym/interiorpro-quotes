package com.example.core.sync.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceRepositoryImpl : DeviceRepository {
    
    // In-memory simulation for architectural foundation
    private val devices = mutableMapOf<String, DeviceInfo>()
    private var localDeviceId: String? = null

    override suspend fun saveDevice(device: DeviceInfo) = withContext(Dispatchers.IO) {
        devices[device.deviceId] = device
    }

    override suspend fun getDevice(deviceId: String): DeviceInfo? = withContext(Dispatchers.IO) {
        devices[deviceId]
    }

    override suspend fun getAllDevices(): List<DeviceInfo> = withContext(Dispatchers.IO) {
        devices.values.toList()
    }

    override suspend fun getLocalDeviceId(): String? = withContext(Dispatchers.IO) {
        localDeviceId
    }

    override suspend fun setLocalDeviceId(deviceId: String) = withContext(Dispatchers.IO) {
        localDeviceId = deviceId
    }

    override suspend fun deleteDevice(deviceId: String) {
        withContext(Dispatchers.IO) {
            devices.remove(deviceId)
        }
    }
}
