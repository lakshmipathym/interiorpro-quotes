package com.example.core.sync.import

data class ImportSummary(
    val importId: String,
    val success: Boolean,
    val date: Long,
    val packageSize: Long,
    val itemsImported: Int,
    val durationMillis: Long,
    val importMode: String, // MERGE, REPLACE, PARTIAL
    val errorMessage: String? = null
)
