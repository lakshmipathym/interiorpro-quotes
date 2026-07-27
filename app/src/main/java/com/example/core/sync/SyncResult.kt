package com.example.core.sync

/**
 * SyncResult defines the outcome of a synchronization operation.
 */
sealed interface SyncResult {
    data class Success(val syncedItemsCount: Int) : SyncResult
    data class Failure(val reason: String) : SyncResult
}
