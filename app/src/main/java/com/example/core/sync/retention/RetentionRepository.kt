package com.example.core.sync.retention

import kotlinx.coroutines.flow.Flow

interface RetentionRepository {
    fun getPolicy(): Flow<RetentionPolicy>
    suspend fun savePolicy(policy: RetentionPolicy)
}
