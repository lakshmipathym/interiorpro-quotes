package com.example.core.sync

import java.util.Date
import com.example.core.backup.BackupMetadata

/**
 * SyncState defines the current state of the Google Drive synchronization process.
 * This state can be collected as a StateFlow inside the UI layer.
 */
sealed interface SyncState {
    object NotConnected : SyncState
    object Connected : SyncState
    object Uploading : SyncState
    object Downloading : SyncState
    object Syncing : SyncState
    data class Success(val lastSyncTime: Date) : SyncState
    data class Failed(val reason: String, val throwable: Throwable? = null) : SyncState
    object Conflict : SyncState
    object WaitingForInternet : SyncState
    object Idle : SyncState
    data class RestoreReady(val backupMetadata: BackupMetadata) : SyncState
}
