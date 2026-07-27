package com.example.core.sync.monitor

import kotlinx.coroutines.flow.Flow

interface HealthMonitorProvider {
    fun observeHealthReport(): Flow<WorkspaceHealthReport>
    suspend fun performHealthCheck()
}
