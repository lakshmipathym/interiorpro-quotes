package com.example.core.backup

import java.io.File

data class WorkspacePreview(
    val isValid: Boolean,
    val errorReason: String? = null,
    val companyName: String = "",
    val quotationCount: Int = 0,
    val customerCount: Int = 0,
    val hasLogo: Boolean = false,
    val hasSignature: Boolean = false,
    val hasSeal: Boolean = false,
    val rawJsonText: String = ""
)

/**
 * WorkspaceManager bundles all app parameters, settings, and physical assets (logo, signature, seal, etc.)
 * into single packages for backup, and safely unpacks them back.
 */
interface WorkspaceManager {
    /**
     * Packages and exports all business assets, profiles, settings, themes, and image assets.
     */
    suspend fun exportWorkspace(destinationFile: File): File

    /**
     * Verifies the workspace file signature, checksum, encryption, and extracts a preview summary.
     */
    suspend fun verifyAndPreviewWorkspace(workspaceBundleFile: File): WorkspacePreview

    /**
     * Imports the validated workspace JSON text into the local database and files.
     */
    suspend fun importWorkspace(rawJsonText: String): Boolean

    /**
     * Standard restore of a workspace bundle.
     */
    suspend fun restoreWorkspace(workspaceBundleFile: File): Boolean
}
