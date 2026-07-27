package com.example.core.backup.policy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupPolicyManagerImpl(
    private val repository: BackupPolicyRepository
) : BackupPolicyManager {

    private val _isEnabledFlow = MutableStateFlow(true)
    override val isEnabledFlow: StateFlow<Boolean> = _isEnabledFlow.asStateFlow()

    private val _activePolicyFlow = MutableStateFlow<BackupPolicy?>(null)
    override val activePolicyFlow: StateFlow<BackupPolicy?> = _activePolicyFlow.asStateFlow()

    private val _availablePoliciesFlow = MutableStateFlow<List<BackupPolicy>>(emptyList())
    override val availablePoliciesFlow: StateFlow<List<BackupPolicy>> = _availablePoliciesFlow.asStateFlow()

    private val policyRegistry = mutableMapOf<String, BackupPolicy>()

    init {
        // Register default policies
        registerPolicy(ManualBackupPolicy())
        registerPolicy(BackupAfterSavePolicy())
        registerPolicy(DailyBackupPolicy())
        registerPolicy(WeeklyBackupPolicy())
        registerPolicy(MonthlyBackupPolicy())
        
        loadPolicyState()
    }

    override fun isEnabled(): Boolean {
        return _isEnabledFlow.value
    }

    override fun setEnabled(enabled: Boolean) {
        _isEnabledFlow.value = enabled
        repository.setEnabled(enabled)
    }

    override fun getAvailablePolicies(): List<BackupPolicy> {
        return policyRegistry.values.toList()
    }

    override fun getActivePolicy(): BackupPolicy? {
        return _activePolicyFlow.value
    }

    override fun setActivePolicy(policyId: String): Boolean {
        val policy = policyRegistry[policyId] ?: return false
        _activePolicyFlow.value = policy
        repository.setActivePolicyId(policyId)
        return true
    }

    override fun registerPolicy(policy: BackupPolicy) {
        policyRegistry[policy.id] = policy
        _availablePoliciesFlow.value = policyRegistry.values.toList()
    }

    override fun deregisterPolicy(policyId: String): Boolean {
        if (policyRegistry.remove(policyId) != null) {
            _availablePoliciesFlow.value = policyRegistry.values.toList()
            if (_activePolicyFlow.value?.id == policyId) {
                setActivePolicy("MANUAL")
            }
            return true
        }
        return false
    }

    override fun shouldTriggerBackup(context: Map<String, Any>): Boolean {
        if (!isEnabled()) return false
        val activePolicy = getActivePolicy() ?: return false
        
        val fullContext = context.toMutableMap()
        if (!fullContext.containsKey("lastBackupTime")) {
            fullContext["lastBackupTime"] = repository.getLastBackupTime()
        }
        if (!fullContext.containsKey("currentTime")) {
            fullContext["currentTime"] = System.currentTimeMillis()
        }
        
        return activePolicy.shouldTrigger(fullContext)
    }

    override fun recordBackupSuccess(timestamp: Long) {
        repository.setLastBackupTime(timestamp)
    }

    override fun savePolicyState() {
        repository.setEnabled(isEnabled())
        repository.setActivePolicyId(getActivePolicy()?.id)
    }

    override fun loadPolicyState() {
        val enabled = repository.isEnabled()
        _isEnabledFlow.value = enabled
        
        val activeId = repository.getActivePolicyId() ?: "MANUAL"
        val activePolicy = policyRegistry[activeId] ?: policyRegistry["MANUAL"]
        _activePolicyFlow.value = activePolicy
    }
}
