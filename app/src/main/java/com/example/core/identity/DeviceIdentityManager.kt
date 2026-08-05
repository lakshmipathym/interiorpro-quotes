package com.example.core.identity

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class DeviceIdentityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("identity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
    }

    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrEmpty()) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
}
