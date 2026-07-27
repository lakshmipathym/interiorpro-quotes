package com.example.core.sync.device

interface DeviceRegistrationProvider {
    suspend fun registerCurrentDevice(customName: String? = null): DeviceRegistrationResult
}
