package com.example.core.sync.import

data class ImportMetadata(
    val exportId: String,
    val date: Long,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val isEncrypted: Boolean,
    val isCompressed: Boolean,
    val formatVersion: Int
)
