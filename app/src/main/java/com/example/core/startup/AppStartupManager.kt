package com.example.core.startup

import android.content.Context
import com.example.core.license.CloudLicenseValidator
import com.example.core.license.LicenseManager
import com.example.core.license.LicenseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppStartupManager(private val context: Context) {
    private val licenseManager = LicenseManager(context)
    private val cloudLicenseValidator = CloudLicenseValidator(context)

    suspend fun runStartupFlow(): LicenseState {
        licenseManager.initTrialIfNeeded()
        
        // Launch non-blocking background verification if network is accessible
        CoroutineScope(Dispatchers.IO).launch {
            try {
                cloudLicenseValidator.verifyOrRegisterCloudLicense()
            } catch (e: Exception) {
                // Ignore network errors on startup to maintain offline resilience
            }
        }

        delay(800) // Brief splash delay
        return licenseManager.getLicenseState()
    }

    fun getLicenseManager(): LicenseManager = licenseManager
    fun getCloudLicenseValidator(): CloudLicenseValidator = cloudLicenseValidator
}


