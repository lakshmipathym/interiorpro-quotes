package com.example.core.sync.dashboard

data class WorkspaceHealth(
    val isBackupReady: Boolean,
    val isRestoreReady: Boolean,
    val isSyncReady: Boolean,
    val isPdfEngineReady: Boolean,
    val isDatabaseHealthy: Boolean
)
