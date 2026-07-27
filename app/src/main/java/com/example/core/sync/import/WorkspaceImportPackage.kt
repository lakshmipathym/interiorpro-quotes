package com.example.core.sync.import

data class WorkspaceImportPackage(
    val metadata: ImportMetadata,
    val manifest: Map<String, String>,
    val databasePath: String?,
    val assets: List<String>,
    val settings: Map<String, Any>
)
