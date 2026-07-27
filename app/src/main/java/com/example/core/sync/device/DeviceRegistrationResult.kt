package com.example.core.sync.device

sealed class DeviceRegistrationResult {
    data class Success(val device: CurrentDevice) : DeviceRegistrationResult()
    data class Failure(val reason: String) : DeviceRegistrationResult()
}
