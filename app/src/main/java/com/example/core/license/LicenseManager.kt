package com.example.core.license

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

enum class LicenseState {
    VALID_TRIAL,
    EXPIRED_TRIAL,
    LICENSED
}

class LicenseManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("license_prefs", Context.MODE_PRIVATE)

    companion object {
        const val TRIAL_DURATION_DAYS = 30L
        private const val KEY_FIRST_INSTALL_DATE = "first_install_date"
        private const val KEY_TRIAL_START_DATE = "trial_start_date"
        private const val KEY_TRIAL_END_DATE = "trial_end_date"
        private const val KEY_IS_LICENSED = "is_licensed"
    }

    init {
        initTrialIfNeeded()
    }

    fun initTrialIfNeeded() {
        if (!prefs.contains(KEY_FIRST_INSTALL_DATE)) {
            val now = System.currentTimeMillis()
            val trialEnd = now + TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS)
            prefs.edit()
                .putLong(KEY_FIRST_INSTALL_DATE, now)
                .putLong(KEY_TRIAL_START_DATE, now)
                .putLong(KEY_TRIAL_END_DATE, trialEnd)
                .apply()
        }
    }

    fun getFirstInstallDate(): Long {
        return prefs.getLong(KEY_FIRST_INSTALL_DATE, System.currentTimeMillis())
    }

    fun getTrialStartDate(): Long {
        return prefs.getLong(KEY_TRIAL_START_DATE, System.currentTimeMillis())
    }

    fun getTrialEndDate(): Long {
        val now = System.currentTimeMillis()
        return prefs.getLong(KEY_TRIAL_END_DATE, now + TimeUnit.DAYS.toMillis(TRIAL_DURATION_DAYS))
    }

    fun getRemainingDays(): Long {
        val diff = getTrialEndDate() - System.currentTimeMillis()
        if (diff <= 0) return 0L
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun isTrialExpired(): Boolean {
        return System.currentTimeMillis() > getTrialEndDate()
    }

    fun isLicensed(): Boolean {
        return prefs.getBoolean(KEY_IS_LICENSED, false)
    }

    fun setLicensed(isLicensed: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LICENSED, isLicensed).apply()
    }

    fun getLicenseState(): LicenseState {
        if (isLicensed()) {
            return LicenseState.LICENSED
        }
        return if (isTrialExpired()) {
            LicenseState.EXPIRED_TRIAL
        } else {
            LicenseState.VALID_TRIAL
        }
    }
}

