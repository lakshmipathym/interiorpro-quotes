package com.example.core.sync.retention

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RetentionRepositoryImpl : RetentionRepository {
    private val _policy = MutableStateFlow(RetentionPolicy())

    override fun getPolicy(): Flow<RetentionPolicy> = _policy.asStateFlow()

    override suspend fun savePolicy(policy: RetentionPolicy) {
        _policy.value = policy
    }
}
