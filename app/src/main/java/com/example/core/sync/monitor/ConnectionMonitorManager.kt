package com.example.core.sync.monitor

import android.util.Log
import kotlinx.coroutines.flow.Flow

class ConnectionMonitorManager(
    private val repository: ConnectionRepository
) : ConnectionMonitorProvider {

    companion object {
        private const val TAG = "ConnectionMonitorManager"
    }

    override fun observeConnectionState(): Flow<ConnectionState> {
        return repository.getConnectionState()
    }

    override suspend fun refreshConnectionStatus() {
        Log.i(TAG, "Refreshing connection status...")
        // In the future, this will check actual network/Google APIs
        // For architectural prep, we simulate checking connection statuses
        repository.updateNetworkStatus(NetworkStatus.WIFI, true)
        repository.updateGoogleAccountStatus(GoogleAccountStatus.SIGNED_IN)
        repository.updateDriveStatus(DriveStatus.AVAILABLE)
        repository.updateBackgroundSyncReady(true)
        Log.i(TAG, "Connection status refreshed.")
    }
}
