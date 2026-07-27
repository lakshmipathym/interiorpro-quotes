package com.example.core.sync.validation

data class PackageStructure(
    val hasManifest: Boolean,
    val hasMetadata: Boolean,
    val hasDatabase: Boolean,
    val hasAssets: Boolean,
    val hasLogo: Boolean,
    val hasSignature: Boolean,
    val hasCompanySeal: Boolean,
    val hasTheme: Boolean,
    val hasSettings: Boolean
)
