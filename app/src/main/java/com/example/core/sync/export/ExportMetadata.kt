package com.example.core.sync.export

data class ExportMetadata(
    val exportId: String,
    val date: Long,
    val deviceName: String,
    val appVersion: String,
    val databaseVersion: Int,
    val isEncrypted: Boolean,
    val isCompressed: Boolean,
    val formatVersion: Int
)
