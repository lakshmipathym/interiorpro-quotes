package com.example.core.sync.status

enum class SyncState {
    IDLE,
    PREPARING,
    UPLOADING,
    DOWNLOADING,
    SYNCING,
    COMPLETED,
    FAILED,
    CANCELLED,
    WAITING_FOR_INTERNET
}
