package com.example.core.sync.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DashboardStateManager(
    private val repository: DashboardRepository
) {
    fun getDashboardState(): Flow<DashboardState> {
        return combine(
            repository.getSyncSummary(),
            repository.getWorkspaceHealth()
        ) { summary, health ->
            DashboardState.Success(summary, health)
        }
    }
}
