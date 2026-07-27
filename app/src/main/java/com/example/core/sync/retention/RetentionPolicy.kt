package com.example.core.sync.retention

data class RetentionPolicy(
    val keepLastNBackups: Int? = null,
    val keepDailyBackups: Int? = null,
    val keepWeeklyBackups: Int? = null,
    val keepMonthlyBackups: Int? = null,
    val neverDeleteFavorites: Boolean = true,
    val neverDeleteLatest: Boolean = true,
    // Future properties for extended features
    val autoCleanupEnabled: Boolean = false,
    val applyToLocal: Boolean = true,
    val applyToCloud: Boolean = false
)
