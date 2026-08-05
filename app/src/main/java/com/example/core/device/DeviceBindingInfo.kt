package com.example.core.device

enum class DeviceStatus {
    UNREGISTERED,
    REGISTERED
}

data class DeviceBindingInfo(
    val deviceFingerprint: String,
    val deviceId: String,
    val installationId: String,
    val workspaceId: String,
    val googleAccountEmail: String? = null,
    val firstRegisteredDate: Long,
    val lastSeenDate: Long,
    val appVersion: String,
    val deviceStatus: DeviceStatus = DeviceStatus.REGISTERED
)
