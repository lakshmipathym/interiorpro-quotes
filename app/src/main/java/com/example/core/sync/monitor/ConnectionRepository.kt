package com.example.core.sync.monitor

import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun getConnectionState(): Flow<ConnectionState>
    suspend fun updateNetworkStatus(status: NetworkStatus, isAvailable: Boolean)
    suspend fun updateGoogleAccountStatus(status: GoogleAccountStatus)
    suspend fun updateDriveStatus(status: DriveStatus)
    suspend fun updateBackgroundSyncReady(isReady: Boolean)
}
