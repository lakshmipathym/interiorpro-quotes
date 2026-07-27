package com.example.core.sync.retention

import kotlinx.coroutines.flow.Flow

interface RetentionPolicyProvider {
    fun observePolicy(): Flow<RetentionPolicy>
    suspend fun updatePolicy(policy: RetentionPolicy)
    suspend fun resetToDefault()
}
