package com.example.core.backup.policy

import android.content.Context
import android.content.SharedPreferences

interface BackupPolicyRepository {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
    fun getActivePolicyId(): String?
    fun setActivePolicyId(policyId: String?)
    fun getLastBackupTime(): Long
    fun setLastBackupTime(timestamp: Long)
}

class BackupPolicyRepositoryImpl(context: Context) : BackupPolicyRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("backup_policy_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "policy_enabled"
        private const val KEY_ACTIVE_POLICY_ID = "active_policy_id"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    }

    override fun isEnabled(): Boolean {
        return prefs.getBoolean(KEY_ENABLED, true)
    }

    override fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override fun getActivePolicyId(): String? {
        return prefs.getString(KEY_ACTIVE_POLICY_ID, "MANUAL")
    }

    override fun setActivePolicyId(policyId: String?) {
        prefs.edit().putString(KEY_ACTIVE_POLICY_ID, policyId).apply()
    }

    override fun getLastBackupTime(): Long {
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    override fun setLastBackupTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()
    }
}
