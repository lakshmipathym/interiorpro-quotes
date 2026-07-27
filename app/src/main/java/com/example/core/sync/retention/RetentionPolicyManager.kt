package com.example.core.sync.retention

import kotlinx.coroutines.flow.Flow

class RetentionPolicyManager(
    private val repository: RetentionRepository
) : RetentionPolicyProvider {

    override fun observePolicy(): Flow<RetentionPolicy> = repository.getPolicy()

    override suspend fun updatePolicy(policy: RetentionPolicy) {
        repository.savePolicy(policy)
    }

    override suspend fun resetToDefault() {
        repository.savePolicy(RetentionPolicy())
    }
}
