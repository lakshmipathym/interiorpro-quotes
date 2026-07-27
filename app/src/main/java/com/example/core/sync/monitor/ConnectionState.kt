package com.example.core.sync.monitor

data class ConnectionState(
    val networkStatus: NetworkStatus,
    val isInternetAvailable: Boolean,
    val googleAccountStatus: GoogleAccountStatus,
    val driveStatus: DriveStatus,
    val isBackgroundSyncReady: Boolean
)
