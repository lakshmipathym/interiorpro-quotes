package com.example.core.sync.monitor

import kotlinx.coroutines.flow.Flow

interface ConnectionMonitorProvider {
    fun observeConnectionState(): Flow<ConnectionState>
    suspend fun refreshConnectionStatus()
}
