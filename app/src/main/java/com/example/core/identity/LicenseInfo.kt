package com.example.core.identity

data class LicenseInfo(
    val licenseStatus: String,
    val trialStatus: String,
    val trialStart: Long,
    val trialEnd: Long,
    val activationDate: Long? = null,
    val expiryDate: Long? = null,
    val deviceId: String,
    val installationId: String,
    val workspaceId: String,
    val googleAccountEmail: String? = null,
    val licenseKey: String? = null
)
