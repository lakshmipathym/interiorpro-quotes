package com.example.core.license

import android.content.Context
import android.content.SharedPreferences
import com.example.core.device.DeviceFingerprintProvider
import com.example.core.identity.AppIdentityProvider
import com.example.core.identity.DeviceIdentityManager
import com.example.core.identity.GoogleIdentityManager
import com.example.core.identity.InstallationIdentity
import com.example.core.identity.WorkspaceIdentity
import com.example.core.network.ApiResult
import com.example.core.network.AppsScriptApiClient
import com.example.core.network.BaseApiRequest
import com.example.core.network.CloudLicenseDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

data class CloudLicenseState(
    val isVerifiedOnline: Boolean = false,
    val isGracePeriodActive: Boolean = false,
    val plan: String = "LOCAL_TRIAL",
    val status: String = "ACTIVE",
    val licenseKey: String? = null,
    val expiryDateIso: String? = null,
    val remainingDays: Long = 30L,
    val lastVerificationTimestamp: Long = 0L,
    val syncStatusMessage: String = "Not Synced"
)

class CloudLicenseValidator(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cloud_license_prefs", Context.MODE_PRIVATE)
    private val apiClient = AppsScriptApiClient(context)

    private val deviceIdentityManager = DeviceIdentityManager(context)
    private val installationIdentity = InstallationIdentity(context)
    private val workspaceIdentity = WorkspaceIdentity(context)
    private val fingerprintProvider = DeviceFingerprintProvider(context)
    private val appIdentityProvider = AppIdentityProvider(context)
    private val licenseManager = LicenseManager(context)

    companion object {
        private const val KEY_PLAN = "cloud_plan"
        private const val KEY_STATUS = "cloud_status"
        private const val KEY_LICENSE_KEY = "cloud_license_key"
        private const val KEY_EXPIRY_DATE = "cloud_expiry_date"
        private const val KEY_REMAINING_DAYS = "cloud_remaining_days"
        private const val KEY_LAST_VERIFIED = "cloud_last_verified_ts"
        const val OFFLINE_GRACE_PERIOD_DAYS = 30L
    }

    private val _cloudState = MutableStateFlow<CloudLicenseState>(getCachedState())
    val cloudState: StateFlow<CloudLicenseState> = _cloudState.asStateFlow()

    fun getCachedState(): CloudLicenseState {
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0L)
        val now = System.currentTimeMillis()
        val daysSinceVerified = if (lastVerified > 0L) TimeUnit.MILLISECONDS.toDays(now - lastVerified) else 0L
        val isGraceActive = lastVerified > 0L && daysSinceVerified in 1..OFFLINE_GRACE_PERIOD_DAYS

        return CloudLicenseState(
            isVerifiedOnline = lastVerified > 0L && daysSinceVerified == 0L,
            isGracePeriodActive = isGraceActive,
            plan = prefs.getString(KEY_PLAN, "LOCAL_TRIAL") ?: "LOCAL_TRIAL",
            status = prefs.getString(KEY_STATUS, "ACTIVE") ?: "ACTIVE",
            licenseKey = prefs.getString(KEY_LICENSE_KEY, null),
            expiryDateIso = prefs.getString(KEY_EXPIRY_DATE, null),
            remainingDays = prefs.getLong(KEY_REMAINING_DAYS, licenseManager.getRemainingDays()),
            lastVerificationTimestamp = lastVerified,
            syncStatusMessage = if (lastVerified == 0L) "Not Synced" else if (isGraceActive) "Offline (Grace Period Active)" else "Verified Online"
        )
    }

    fun loadCachedState(): CloudLicenseState {
        val state = getCachedState()
        _cloudState.value = state
        return state
    }

    suspend fun verifyOrRegisterCloudLicense(): CloudLicenseState {
        val googleIdentityManager = GoogleIdentityManager(
            context,
            com.example.core.drive.GoogleSignInManagerImpl(context)
        )
        val googleEmail = googleIdentityManager.getConnectedAccount()?.email

        val request = BaseApiRequest(
            action = "verify",
            deviceId = deviceIdentityManager.getDeviceId(),
            installationId = installationIdentity.getInstallationId(),
            workspaceId = workspaceIdentity.getWorkspaceId(),
            deviceFingerprint = fingerprintProvider.generateFingerprint(),
            appVersion = appIdentityProvider.getAppVersion(),
            email = googleEmail
        )

        when (val result = apiClient.sendRequest(request)) {
            is ApiResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    val plan = data.optString("plan", "COMMERCIAL_ANNUAL")
                    val status = data.optString("status", "ACTIVE")
                    val licenseKey = data.optString("licenseKey", null)
                    val expiryDate = data.optString("expiryDate", null)
                    val remainingDays = data.optLong("remainingDays", 30L)
                    val now = System.currentTimeMillis()

                    saveCache(plan, status, licenseKey, expiryDate, remainingDays, now)
                    if (status.equals("ACTIVE", ignoreCase = true)) {
                        licenseManager.setLicensed(true)
                    }

                    val updatedState = CloudLicenseState(
                        isVerifiedOnline = true,
                        isGracePeriodActive = false,
                        plan = plan,
                        status = status,
                        licenseKey = licenseKey,
                        expiryDateIso = expiryDate,
                        remainingDays = remainingDays,
                        lastVerificationTimestamp = now,
                        syncStatusMessage = "Verified Online"
                    )
                    _cloudState.value = updatedState
                    return updatedState
                }
            }
            is ApiResult.Error -> {
                if (result.code == 404) {
                    // Try auto registration if not found on backend
                    return performCloudRegistration(googleEmail)
                }
            }
            is ApiResult.NetworkError -> {
                // Offline fallback - use cached state
            }
        }

        return loadCachedState()
    }

    suspend fun activateSubscriptionPlan(
        plan: SubscriptionPlan,
        customLicenseKey: String? = null
    ): CloudLicenseState {
        val googleIdentityManager = GoogleIdentityManager(
            context,
            com.example.core.drive.GoogleSignInManagerImpl(context)
        )
        val googleEmail = googleIdentityManager.getConnectedAccount()?.email

        val assignedKey = if (!customLicenseKey.isNullOrBlank()) {
            customLicenseKey.trim()
        } else {
            "LIC-${plan.name}-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
        }

        val request = BaseApiRequest(
            action = "renew",
            deviceId = deviceIdentityManager.getDeviceId(),
            installationId = installationIdentity.getInstallationId(),
            workspaceId = workspaceIdentity.getWorkspaceId(),
            deviceFingerprint = fingerprintProvider.generateFingerprint(),
            appVersion = appIdentityProvider.getAppVersion(),
            email = googleEmail,
            licenseKey = assignedKey
        )

        when (val result = apiClient.sendRequest(request)) {
            is ApiResult.Success -> {
                val data = result.data.data
                val now = System.currentTimeMillis()
                val newExpiry = data?.optString("newExpiryDate", null)
                val status = data?.optString("status", "ACTIVE") ?: "ACTIVE"

                saveCache(plan.planCode, status, assignedKey, newExpiry, plan.durationDays, now)
                licenseManager.setLicensed(true)

                val updatedState = CloudLicenseState(
                    isVerifiedOnline = true,
                    isGracePeriodActive = false,
                    plan = plan.planCode,
                    status = status,
                    licenseKey = assignedKey,
                    expiryDateIso = newExpiry,
                    remainingDays = plan.durationDays,
                    lastVerificationTimestamp = now,
                    syncStatusMessage = "${plan.displayName} Activated"
                )
                _cloudState.value = updatedState
                return updatedState
            }
            is ApiResult.Error -> {
                return performCloudRegistrationWithPlan(googleEmail, plan, assignedKey)
            }
            is ApiResult.NetworkError -> {
                val now = System.currentTimeMillis()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                val expiryIso = sdf.format(java.util.Date(now + java.util.concurrent.TimeUnit.DAYS.toMillis(plan.durationDays)))

                saveCache(plan.planCode, "ACTIVE", assignedKey, expiryIso, plan.durationDays, now)
                licenseManager.setLicensed(true)

                val offlineState = CloudLicenseState(
                    isVerifiedOnline = false,
                    isGracePeriodActive = true,
                    plan = plan.planCode,
                    status = "ACTIVE",
                    licenseKey = assignedKey,
                    expiryDateIso = expiryIso,
                    remainingDays = plan.durationDays,
                    lastVerificationTimestamp = now,
                    syncStatusMessage = "${plan.displayName} (Offline Saved)"
                )
                _cloudState.value = offlineState
                return offlineState
            }
        }
    }

    private suspend fun performCloudRegistrationWithPlan(
        email: String?,
        plan: SubscriptionPlan,
        customKey: String
    ): CloudLicenseState {
        val request = BaseApiRequest(
            action = "register",
            deviceId = deviceIdentityManager.getDeviceId(),
            installationId = installationIdentity.getInstallationId(),
            workspaceId = workspaceIdentity.getWorkspaceId(),
            deviceFingerprint = fingerprintProvider.generateFingerprint(),
            appVersion = appIdentityProvider.getAppVersion(),
            email = email,
            licenseKey = customKey
        )

        when (val result = apiClient.sendRequest(request)) {
            is ApiResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    val status = data.optString("Status", "ACTIVE")
                    val licenseKey = data.optString("LicenseKey", customKey)
                    val expiryDate = data.optString("ExpiryDate", null)
                    val now = System.currentTimeMillis()

                    saveCache(plan.planCode, status, licenseKey, expiryDate, plan.durationDays, now)
                    licenseManager.setLicensed(true)

                    val newState = CloudLicenseState(
                        isVerifiedOnline = true,
                        isGracePeriodActive = false,
                        plan = plan.planCode,
                        status = status,
                        licenseKey = licenseKey,
                        expiryDateIso = expiryDate,
                        remainingDays = plan.durationDays,
                        lastVerificationTimestamp = now,
                        syncStatusMessage = "Plan Registered & Verified"
                    )
                    _cloudState.value = newState
                    return newState
                }
            }
            else -> {}
        }
        return loadCachedState()
    }

    private suspend fun performCloudRegistration(email: String?): CloudLicenseState {
        val request = BaseApiRequest(
            action = "register",
            deviceId = deviceIdentityManager.getDeviceId(),
            installationId = installationIdentity.getInstallationId(),
            workspaceId = workspaceIdentity.getWorkspaceId(),
            deviceFingerprint = fingerprintProvider.generateFingerprint(),
            appVersion = appIdentityProvider.getAppVersion(),
            email = email
        )

        when (val result = apiClient.sendRequest(request)) {
            is ApiResult.Success -> {
                val data = result.data.data
                if (data != null) {
                    val plan = data.optString("Plan", "TRIAL_30_DAYS")
                    val status = data.optString("Status", "ACTIVE")
                    val licenseKey = data.optString("LicenseKey", null)
                    val expiryDate = data.optString("ExpiryDate", null)
                    val now = System.currentTimeMillis()

                    saveCache(plan, status, licenseKey, expiryDate, 30L, now)

                    val newState = CloudLicenseState(
                        isVerifiedOnline = true,
                        isGracePeriodActive = false,
                        plan = plan,
                        status = status,
                        licenseKey = licenseKey,
                        expiryDateIso = expiryDate,
                        remainingDays = 30L,
                        lastVerificationTimestamp = now,
                        syncStatusMessage = "Registered & Verified Online"
                    )
                    _cloudState.value = newState
                    return newState
                }
            }
            else -> {}
        }
        return loadCachedState()
    }

    private fun saveCache(
        plan: String,
        status: String,
        licenseKey: String?,
        expiryDate: String?,
        remainingDays: Long,
        timestamp: Long
    ) {
        prefs.edit()
            .putString(KEY_PLAN, plan)
            .putString(KEY_STATUS, status)
            .putString(KEY_LICENSE_KEY, licenseKey)
            .putString(KEY_EXPIRY_DATE, expiryDate)
            .putLong(KEY_REMAINING_DAYS, remainingDays)
            .putLong(KEY_LAST_VERIFIED, timestamp)
            .apply()
    }
}
