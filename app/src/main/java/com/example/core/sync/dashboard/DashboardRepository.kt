package com.example.core.sync.dashboard

import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getSyncSummary(): Flow<SyncSummary>
    fun getWorkspaceHealth(): Flow<WorkspaceHealth>
}
