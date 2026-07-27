package com.example.core.sync.dashboard

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val syncSummary: SyncSummary, val workspaceHealth: WorkspaceHealth) : DashboardState()
    data class Error(val message: String) : DashboardState()
}
