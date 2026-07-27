package com.example.core.drive.upload

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

class DriveUploadManager(
    private val context: Context,
    private val repository: DriveUploadRepository
) : DriveUploadProvider {

    companion object {
        private const val TAG = "DriveUploadManager"
        private const val UPLOAD_TIMEOUT_MS = 60000L // 60 seconds
    }

    private val _progress = MutableStateFlow<UploadProgress?>(null)
    override val progress: StateFlow<UploadProgress?> = _progress.asStateFlow()

    override suspend fun uploadPackage(request: UploadRequest): UploadResult {
        Log.i(TAG, "Starting Google Drive backup upload flow...")
        _progress.value = UploadProgress(0L, 0L, 0.0f, "VALIDATING")

        // 1. Validate package
        val backupPackage = request.backupPackage
        if (backupPackage.encryptedPayload.isEmpty()) {
            val err = UploadResult.Failure(UploadError.InvalidPackage, "Empty encrypted payload in BackupPackage")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            return err
        }

        // 2. Verify metadata
        val metadata = backupPackage.manifest.metadata
        if (metadata.backupId.isBlank()) {
            val err = UploadResult.Failure(UploadError.InvalidPackage, "Invalid or empty backupId in metadata")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            return err
        }
        if (metadata.databaseVersion <= 0) {
            val err = UploadResult.Failure(UploadError.InvalidPackage, "Invalid databaseVersion in metadata")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            return err
        }

        // 3. Verify checksum availability
        if (backupPackage.checksum.isBlank()) {
            val err = UploadResult.Failure(UploadError.InvalidPackage, "Checksum is empty or missing")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            return err
        }

        // Check Network Connectivity
        if (!repository.isNetworkAvailable()) {
            val err = UploadResult.Failure(UploadError.NoInternet, "Network connection is not available")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            return err
        }

        // Check Google Drive Authorization
        if (!repository.isDriveAuthorized()) {
            val err = UploadResult.Failure(UploadError.AuthenticationFailure, "Google Drive app access is unauthorized")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            return err
        }

        _progress.value = UploadProgress(0L, backupPackage.encryptedPayload.size.toLong(), 0.0f, "PREPARING")

        // Define a filename
        val fileName = request.customFileName ?: "backup_${metadata.backupId}.bin"

        // Generate temp file in cache directory
        val tempFile = File(context.cacheDir, fileName)

        return try {
            _progress.value = UploadProgress(0L, backupPackage.encryptedPayload.size.toLong(), 10.0f, "UPLOADING")

            val fileId = withTimeout(UPLOAD_TIMEOUT_MS) {
                repository.uploadToAppData(
                    backupPackage = backupPackage,
                    tempFile = tempFile,
                    metadata = mapOf("fileName" to fileName)
                )
            }

            val totalBytes = tempFile.length()
            _progress.value = UploadProgress(totalBytes, totalBytes, 100.0f, "COMPLETED")

            Log.i(TAG, "Successfully uploaded backup payload to Google Drive App Data Folder: ID = $fileId")
            UploadResult.Success(fileId, fileName, totalBytes)

        } catch (e: CancellationException) {
            Log.w(TAG, "Google Drive upload was cancelled")
            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            UploadResult.Failure(UploadError.UploadCancelled, "Upload request was explicitly cancelled")
        } catch (e: TimeoutException) {

            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            UploadResult.Failure(UploadError.UploadTimeout, "Upload timed out after ${UPLOAD_TIMEOUT_MS / 1000}s")
        } catch (e: SocketTimeoutException) {

            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            UploadResult.Failure(UploadError.UploadTimeout, "Network connection timed out during upload")
        } catch (e: IOException) {

            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            
            val message = e.message ?: ""
            if (message.contains("403") || message.contains("Permission", ignoreCase = true)) {
                UploadResult.Failure(UploadError.DrivePermissionFailure, "Google Drive permissions are insufficient")
            } else if (message.contains("401") || message.contains("Unauthorized", ignoreCase = true)) {
                UploadResult.Failure(UploadError.AuthenticationFailure, "Authorization token expired or invalid")
            } else {
                UploadResult.Failure(
                    UploadError.GoogleApiFailure(500, "Google Drive REST operation failed: $message"),
                    "API interaction failed"
                )
            }
        } catch (e: Exception) {

            _progress.value = UploadProgress(0L, 0L, 0.0f, "FAILED")
            UploadResult.Failure(UploadError.GeneralError("Unknown error"), "Unknown error")
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
