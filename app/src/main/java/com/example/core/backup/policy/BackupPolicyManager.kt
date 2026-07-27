package com.example.core.backup.policy

import kotlinx.coroutines.flow.StateFlow

interface BackupPolicyManager {
    val isEnabledFlow: StateFlow<Boolean>
    val activePolicyFlow: StateFlow<BackupPolicy?>
    val availablePoliciesFlow: StateFlow<List<BackupPolicy>>

    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
    
    fun getAvailablePolicies(): List<BackupPolicy>
    fun getActivePolicy(): BackupPolicy?
    
    fun setActivePolicy(policyId: String): Boolean
    fun registerPolicy(policy: BackupPolicy)
    fun deregisterPolicy(policyId: String): Boolean
    
    fun shouldTriggerBackup(context: Map<String, Any>): Boolean
    fun recordBackupSuccess(timestamp: Long = System.currentTimeMillis())
    
    fun savePolicyState()
    fun loadPolicyState()
}
