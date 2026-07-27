package com.example.core.backup.preview

data class RestoreImpact(
    val totalDifference: RestoreDifference,
    val compatibilityStatus: String
)
