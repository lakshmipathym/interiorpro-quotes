package com.example.core.drive.upload

import kotlinx.coroutines.flow.StateFlow

interface DriveUploadProvider {
    val progress: StateFlow<UploadProgress?>
    suspend fun uploadPackage(request: UploadRequest): UploadResult
}
