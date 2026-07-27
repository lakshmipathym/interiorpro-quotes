package com.example.core.sync.migration

data class MigrationSummary(
    val customerCount: Int,
    val quotationCount: Int,
    val masterDataCount: Int,
    val hasCompanyProfile: Boolean,
    val hasLogo: Boolean,
    val hasSignature: Boolean,
    val hasCompanySeal: Boolean,
    val hasTheme: Boolean,
    val hasSettings: Boolean,
    val backupVersion: Int,
    val appVersion: String,
    val databaseVersion: Int
)
