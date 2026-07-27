package com.example.core.backup.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BackupPolicyViewModel(
    private val policyManager: BackupPolicyManager
) : ViewModel() {

    val isEnabled: StateFlow<Boolean> = policyManager.isEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), policyManager.isEnabled())

    val activePolicy: StateFlow<BackupPolicy?> = policyManager.activePolicyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), policyManager.getActivePolicy())

    val availablePolicies: StateFlow<List<BackupPolicy>> = policyManager.availablePoliciesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), policyManager.getAvailablePolicies())

    fun setEnabled(enabled: Boolean) {
        policyManager.setEnabled(enabled)
    }

    fun selectPolicy(policyId: String): Boolean {
        return policyManager.setActivePolicy(policyId)
    }

    fun registerCustomPolicy(policy: BackupPolicy) {
        policyManager.registerPolicy(policy)
    }
}

class BackupPolicyViewModelFactory(
    private val policyManager: BackupPolicyManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupPolicyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupPolicyViewModel(policyManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
