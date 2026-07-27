package com.example.core.drive.upload

sealed class UploadResult {
    data class Success(
        val fileId: String,
        val fileName: String,
        val sizeBytes: Long
    ) : UploadResult()

    data class Failure(
        val error: UploadError,
        val message: String
    ) : UploadResult()
}

sealed class UploadError {
    object NoInternet : UploadError()
    object AuthenticationFailure : UploadError()
    object DrivePermissionFailure : UploadError()
    object UploadCancelled : UploadError()
    object UploadTimeout : UploadError()
    data class GoogleApiFailure(val code: Int, val apiMessage: String) : UploadError()
    object InvalidPackage : UploadError()
    object RetryReady : UploadError()
    data class GeneralError(val errorMessage: String) : UploadError()

    fun isRetryable(): Boolean {
        return when (this) {
            is NoInternet -> true
            is UploadTimeout -> true
            is RetryReady -> true
            is GoogleApiFailure -> code >= 500
            else -> false
        }
    }
}
