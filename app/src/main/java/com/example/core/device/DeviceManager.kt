package com.example.core.device

/**
 * DeviceManager retrieves device metrics and synchronization timestamps to log session telemetry.
 */
interface DeviceManager {
    fun getDeviceId(): String
    fun getDeviceName(): String
    fun getDeviceModel(): String
    fun getAndroidVersion(): String
    fun getAppVersion(): String
    fun getDatabaseVersion(): Int
    fun getRegistrationTime(): Long
    fun getLastSyncTime(): Long
    fun updateLastSyncTime(timestamp: Long)
}
