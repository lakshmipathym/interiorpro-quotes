package com.example.core.sync.export

data class WorkspaceExportPackage(
    val metadata: ExportMetadata,
    val manifest: Map<String, String>,
    val databasePath: String?,
    val assets: List<String>,
    val settings: Map<String, Any>
)
