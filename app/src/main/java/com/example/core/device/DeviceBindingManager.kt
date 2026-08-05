package com.example.core.device

import android.content.Context
import android.content.SharedPreferences
import com.example.core.identity.AppIdentityProvider
import com.example.core.identity.DeviceIdentityManager
import com.example.core.identity.GoogleIdentityManager
import com.example.core.identity.InstallationIdentity
import com.example.core.identity.WorkspaceIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceBindingManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("device_binding_prefs", Context.MODE_PRIVATE)

    private val fingerprintProvider = DeviceFingerprintProvider(context)
    private val deviceIdentityManager = DeviceIdentityManager(context)
    private val installationIdentity = InstallationIdentity(context)
    private val workspaceIdentity = WorkspaceIdentity(context)
    private val appIdentityProvider = AppIdentityProvider(context)

    companion object {
        private const val KEY_FIRST_REGISTERED_DATE = "first_registered_date"
        private const val KEY_LAST_SEEN_DATE = "last_seen_date"
        private const val KEY_DEVICE_STATUS = "device_status"
    }

    private val _bindingInfo = MutableStateFlow<DeviceBindingInfo>(getOrInitializeBindingInfo())
    val bindingInfo: StateFlow<DeviceBindingInfo> = _bindingInfo.asStateFlow()

    init {
        updateLastSeen()
    }

    fun getBindingInfo(): DeviceBindingInfo {
        return getOrInitializeBindingInfo()
    }

    private fun getOrInitializeBindingInfo(): DeviceBindingInfo {
        val now = System.currentTimeMillis()
        var firstRegistered = prefs.getLong(KEY_FIRST_REGISTERED_DATE, 0L)
        if (firstRegistered == 0L) {
            firstRegistered = now
            prefs.edit().putLong(KEY_FIRST_REGISTERED_DATE, firstRegistered).apply()
        }

        val statusStr = prefs.getString(KEY_DEVICE_STATUS, DeviceStatus.REGISTERED.name) ?: DeviceStatus.REGISTERED.name
        val status = try {
            DeviceStatus.valueOf(statusStr)
        } catch (e: Exception) {
            DeviceStatus.REGISTERED
        }

        val fingerprint = fingerprintProvider.generateFingerprint()
        val deviceId = deviceIdentityManager.getDeviceId()
        val installId = installationIdentity.getInstallationId()
        val workspaceId = workspaceIdentity.getWorkspaceId()
        val appVersion = appIdentityProvider.getAppVersion()
        val lastSeen = prefs.getLong(KEY_LAST_SEEN_DATE, now)

        val googleIdentityManager = GoogleIdentityManager(
            context,
            com.example.core.drive.GoogleSignInManagerImpl(context)
        )
        val googleEmail = googleIdentityManager.getConnectedAccount()?.email

        return DeviceBindingInfo(
            deviceFingerprint = fingerprint,
            deviceId = deviceId,
            installationId = installId,
            workspaceId = workspaceId,
            googleAccountEmail = googleEmail,
            firstRegisteredDate = firstRegistered,
            lastSeenDate = lastSeen,
            appVersion = appVersion,
            deviceStatus = status
        )
    }

    fun updateLastSeen() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SEEN_DATE, now).apply()
        _bindingInfo.value = getOrInitializeBindingInfo()
    }
}
