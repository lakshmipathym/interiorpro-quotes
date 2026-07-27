package com.example.core.sync.search

data class BackupSearchQuery(
    val query: String = "",
    val dateRangeStart: Long? = null,
    val dateRangeEnd: Long? = null,
    val deviceName: String? = null,
    val appVersion: String? = null,
    val databaseVersion: Int? = null,
    val type: String? = null // MANUAL, AUTOMATIC
)
