package com.example.core.sync.export

data class ExportSummary(
    val exportId: String,
    val success: Boolean,
    val packageSize: Long,
    val totalItemsExported: Int,
    val durationMillis: Long,
    val errorMessage: String? = null
)
