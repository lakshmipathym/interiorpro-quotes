package com.example.core.drive.upload

/**
 * UploadProgress represents the progress of an ongoing file upload.
 */
data class UploadProgress(
    val bytesTransferred: Long,
    val totalBytes: Long,
    val percentComplete: Float,
    val status: String
)
