package com.example.core.device

import android.content.Context
import com.example.core.identity.AppIdentityProvider
import com.example.core.identity.DeviceIdentityManager
import com.example.core.identity.InstallationIdentity
import com.example.core.identity.WorkspaceIdentity
import java.security.MessageDigest

class DeviceFingerprintProvider(context: Context) {
    private val deviceIdentityManager = DeviceIdentityManager(context)
    private val installationIdentity = InstallationIdentity(context)
    private val workspaceIdentity = WorkspaceIdentity(context)
    private val appIdentityProvider = AppIdentityProvider(context)

    fun generateFingerprint(): String {
        val deviceId = deviceIdentityManager.getDeviceId()
        val installId = installationIdentity.getInstallationId()
        val workspaceId = workspaceIdentity.getWorkspaceId()
        val manufacturer = appIdentityProvider.getManufacturer()
        val model = appIdentityProvider.getDeviceModel()
        val androidVersion = appIdentityProvider.getAndroidVersion()

        val rawCombined = "$deviceId|$installId|$workspaceId|$manufacturer|$model|$androidVersion"
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawCombined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
