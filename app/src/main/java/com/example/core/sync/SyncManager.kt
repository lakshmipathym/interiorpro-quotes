package com.example.core.sync

import kotlinx.coroutines.flow.StateFlow
import com.example.core.backup.BackupMetadata

/**
 * SyncManager coordinates state tracking and triggers cloud synchronization workflows.
 */
interface SyncManager {
    /**
     * Observable state flow representing the live sync status.
     */
    val syncState: StateFlow<SyncState>

    /**
     * Triggers an explicit sync workflow.
     */
    suspend fun triggerSync(): SyncResult

    /**
     * Resolves metadata and data conflicts between local and cloud backup.
     */
    suspend fun resolveConflicts(preferCloud: Boolean): SyncResult

    /**
     * Clears all recorded sync status and timestamps.
     */
    suspend fun clearSyncState()

    /**
     * Evaluates whether auto backup should trigger after a quotation is saved.
     */
    fun onQuotationSaved()

    /**
     * Checks Google Drive App Data folder for newer backups.
     */
    suspend fun checkForNewerBackup(): BackupMetadata?

    /**
     * Retrieves the current auto backup policy from preferences.
     * Supported values: "MANUAL", "ON_SAVE", "DAILY", "WEEKLY", "MONTHLY"
     */
    fun getAutoBackupPolicy(): String

    /**
     * Sets the auto backup policy.
     */
    fun setAutoBackupPolicy(policy: String)
}
