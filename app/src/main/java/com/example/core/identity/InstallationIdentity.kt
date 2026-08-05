package com.example.core.identity

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class InstallationIdentity(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("identity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_INSTALLATION_ID = "installation_id"
    }

    fun getInstallationId(): String {
        var installId = prefs.getString(KEY_INSTALLATION_ID, null)
        if (installId.isNullOrEmpty()) {
            installId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALLATION_ID, installId).apply()
        }
        return installId
    }
}
