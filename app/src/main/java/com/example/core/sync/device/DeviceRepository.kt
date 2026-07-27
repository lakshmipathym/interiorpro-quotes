package com.example.core.sync.device

interface DeviceRepository {
    suspend fun saveDevice(device: DeviceInfo)
    suspend fun getDevice(deviceId: String): DeviceInfo?
    suspend fun getAllDevices(): List<DeviceInfo>
    suspend fun getLocalDeviceId(): String?
    suspend fun setLocalDeviceId(deviceId: String)
    suspend fun deleteDevice(deviceId: String)
}
