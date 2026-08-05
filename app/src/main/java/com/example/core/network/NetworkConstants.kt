package com.example.core.network

import android.content.Context
import com.example.BuildConfig

object NetworkConstants {
    private const val PREFS_NAME = "network_config_prefs"
    private const val KEY_CUSTOM_URL = "custom_apps_script_url"
    private const val KEY_CUSTOM_SECRET = "custom_shared_secret"

    // Timeout limits in milliseconds
    const val CONNECT_TIMEOUT_MS = 20000
    const val READ_TIMEOUT_MS = 20000

    // Exponential retry configuration: 2s, 5s, 10s
    val RETRY_BACKOFF_DELAYS_MS = longArrayOf(2000L, 5000L, 10000L)
    val MAX_RETRIES = RETRY_BACKOFF_DELAYS_MS.size

    fun getEndpointUrl(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val customUrl = prefs.getString(KEY_CUSTOM_URL, null)
            if (!customUrl.isNullOrBlank()) return customUrl
        }
        return try {
            BuildConfig.APPS_SCRIPT_URL
        } catch (e: Exception) {
            ""
        }
    }

    fun getSecretKey(context: Context? = null): String {
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val customSecret = prefs.getString(KEY_CUSTOM_SECRET, null)
            if (!customSecret.isNullOrBlank()) return customSecret
        }
        return try {
            BuildConfig.APPS_SCRIPT_SECRET
        } catch (e: Exception) {
            ""
        }
    }

    fun updateNetworkConfig(context: Context, customUrl: String?, customSecret: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            if (customUrl != null) putString(KEY_CUSTOM_URL, customUrl) else remove(KEY_CUSTOM_URL)
            if (customSecret != null) putString(KEY_CUSTOM_SECRET, customSecret) else remove(KEY_CUSTOM_SECRET)
            apply()
        }
    }
}
