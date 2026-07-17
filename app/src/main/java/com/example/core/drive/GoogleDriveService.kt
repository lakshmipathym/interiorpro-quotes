package com.example.core.drive

import java.io.File

/**
 * GoogleDriveService handles file storage specifically inside the secure
 * "App Data Folder" on Google Drive, preventing visibility/accidental deletion from normal Drive.
 */
interface GoogleDriveService {
    /**
     * Checks if active authorization token exists.
     */
    suspend fun isAuthorized(): Boolean

    /**
     * Initiates OAuth flow or obtains a valid token.
     */
    suspend fun authorize(): Boolean

    /**
     * Sign out and clear active tokens.
     */
    suspend fun signOut(): Boolean

    /**
     * Uploads a local file to the secure Google Drive App Data Folder.
     */
    suspend fun uploadToAppData(
        file: File,
        mimeType: String,
        metadata: Map<String, String> = emptyMap()
    ): String

    /**
     * Downloads an archive file by ID from the App Data Folder.
     */
    suspend fun downloadFromAppData(fileId: String, destination: File): Boolean

    /**
     * Lists all backup files inside the secure App Data Folder.
     */
    suspend fun listAppDataFiles(): List<DriveFileInfo>

    /**
     * Deletes a file inside the App Data Folder by ID.
     */
    suspend fun deleteFile(fileId: String): Boolean
}

/**
 * Represent metadata of a file stored in Google Drive.
 */
data class DriveFileInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val metadata: Map<String, String>
)
