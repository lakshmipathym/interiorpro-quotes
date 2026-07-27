package com.example.core.sync.migration

data class MigrationRequest(
    val packagePath: String,
    val sourceDeviceName: String,
    val requiresVerification: Boolean = true
)
