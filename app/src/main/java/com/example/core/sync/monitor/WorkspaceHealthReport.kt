package com.example.core.sync.monitor

data class WorkspaceHealthReport(
    val databaseHealth: HealthIndicator,
    val backupEngine: HealthIndicator,
    val restoreEngine: HealthIndicator,
    val syncEngine: HealthIndicator,
    val pdfEngine: HealthIndicator,
    val localStorage: HealthIndicator,
    val googleDriveConnectivity: HealthIndicator,
    val appVersion: String,
    val databaseVersion: Int
)
