package com.example.core.identity

import android.content.Context
import android.os.Build

class AppIdentityProvider(private val context: Context) {

    fun getPackageName(): String = context.packageName

    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    @Suppress("DEPRECATION")
    fun getVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun getAndroidVersion(): String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

    fun getDeviceModel(): String = Build.MODEL ?: "Unknown"

    fun getManufacturer(): String = Build.MANUFACTURER ?: "Unknown"
}
