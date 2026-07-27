package com.example.core.backup.preview

data class RestoreSummary(
    val totalCustomers: Int,
    val totalQuotations: Int,
    val totalMasters: Int,
    val companyProfileAvailable: Boolean,
    val logoAvailable: Boolean,
    val signatureAvailable: Boolean,
    val companySealAvailable: Boolean
)
