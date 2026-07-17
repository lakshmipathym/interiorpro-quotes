package com.example.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings

class DeviceManagerImpl(private val context: Context) : DeviceManager {

    private val prefs = context.getSharedPreferences("interiorpro_device_prefs", Context.MODE_PRIVATE)

    override fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device_id"
        } catch (e: Exception) {
            "unknown_device_id"
        }
    }

    override fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model.capitalize()
        } else {
            "${manufacturer.capitalize()} $model"
        }
    }

    override fun getDeviceModel(): String {
        return Build.MODEL ?: "unknown_model"
    }

    override fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE ?: "unknown_android_version"
    }

    override fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.5"
        } catch (e: Exception) {
            "1.5"
        }
    }

    override fun getDatabaseVersion(): Int {
        return 6 // Aligned with the current database schema version
    }

    override fun getRegistrationTime(): Long {
        var regTime = prefs.getLong("registration_time", 0L)
        if (regTime == 0L) {
            regTime = System.currentTimeMillis()
            prefs.edit().putLong("registration_time", regTime).apply()
        }
        return regTime
    }

    override fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_time", 0L)
    }

    override fun updateLastSyncTime(timestamp: Long) {
        prefs.edit().putLong("last_sync_time", timestamp).apply()
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
