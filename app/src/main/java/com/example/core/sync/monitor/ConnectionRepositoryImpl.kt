package com.example.core.sync.monitor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ConnectionRepositoryImpl : ConnectionRepository {
    private val _connectionState = MutableStateFlow(
        ConnectionState(
            networkStatus = NetworkStatus.UNKNOWN,
            isInternetAvailable = false,
            googleAccountStatus = GoogleAccountStatus.UNKNOWN,
            driveStatus = DriveStatus.UNKNOWN,
            isBackgroundSyncReady = false
        )
    )

    override fun getConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override suspend fun updateNetworkStatus(status: NetworkStatus, isAvailable: Boolean) {
        _connectionState.update { it.copy(networkStatus = status, isInternetAvailable = isAvailable) }
    }

    override suspend fun updateGoogleAccountStatus(status: GoogleAccountStatus) {
        _connectionState.update { it.copy(googleAccountStatus = status) }
    }

    override suspend fun updateDriveStatus(status: DriveStatus) {
        _connectionState.update { it.copy(driveStatus = status) }
    }

    override suspend fun updateBackgroundSyncReady(isReady: Boolean) {
        _connectionState.update { it.copy(isBackgroundSyncReady = isReady) }
    }
}
