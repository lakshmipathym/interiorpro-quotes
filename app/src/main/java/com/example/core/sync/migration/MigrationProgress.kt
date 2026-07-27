package com.example.core.sync.migration

data class MigrationProgress(
    val currentStep: Int,
    val totalSteps: Int,
    val stepDescription: String,
    val percentage: Float
)
