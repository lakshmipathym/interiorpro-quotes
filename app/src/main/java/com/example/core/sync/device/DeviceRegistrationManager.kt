package com.example.core.sync.device

import android.os.Build
import android.util.Log
import java.util.UUID

class DeviceRegistrationManager(
    private val repository: DeviceRepository
) : DeviceRegistrationProvider {

    companion object {
        private const val TAG = "DeviceRegistrationMgr"
    }

    override suspend fun registerCurrentDevice(customName: String?): DeviceRegistrationResult {
        Log.i(TAG, "Registering current device...")
        
        try {
            val existingId = repository.getLocalDeviceId()
            val deviceId = existingId ?: UUID.randomUUID().toString()
            val installId = UUID.randomUUID().toString()
            
            val deviceName = customName ?: "${Build.MANUFACTURER} ${Build.MODEL}"
            
            val deviceInfo = DeviceInfo(
                deviceId = deviceId,
                installId = installId,
                deviceName = deviceName,
                deviceModel = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                appVersion = "1.5.0", // Hardcoded for architectural prep, ideally injected
                databaseVersion = 1,
                registrationDate = System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                lastBackup = null,
                lastSync = null,
                backupSize = 0L,
                status = DeviceStatus.ONLINE,
                isHealthy = true
            )
            
            repository.saveDevice(deviceInfo)
            
            if (existingId == null) {
                repository.setLocalDeviceId(deviceId)
            }
            
            Log.i(TAG, "Device registered successfully: $deviceId")
            return DeviceRegistrationResult.Success(CurrentDevice(deviceInfo))
            
        } catch (e: Exception) {

            return DeviceRegistrationResult.Failure("Failed to register device")
        }
    }
}
