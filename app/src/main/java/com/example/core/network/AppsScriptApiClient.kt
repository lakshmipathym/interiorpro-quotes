package com.example.core.network

import android.content.Context
import com.example.core.security.RequestSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AppsScriptApiClient(
    private val context: Context? = null,
    private val endpointUrl: String? = null
) {

    private fun getTargetUrl(): String {
        return endpointUrl ?: NetworkConstants.getEndpointUrl(context)
    }

    suspend fun sendRequest(request: BaseApiRequest): ApiResult<ApiResponse> = withContext(Dispatchers.IO) {
        val signedRequest = RequestSigner.signRequest(request, context)
        val jsonPayload = signedRequest.toJsonString()
        val targetUrl = getTargetUrl()

        if (targetUrl.isBlank()) {
            return@withContext ApiResult.Error(400, "Google Apps Script endpoint URL is not configured")
        }

        var attempts = 0
        var lastException: Exception? = null
        val delays = NetworkConstants.RETRY_BACKOFF_DELAYS_MS

        while (attempts < delays.size) {
            try {
                val response = executeHttpPost(targetUrl, jsonPayload)
                if (response.isSuccess) {
                    return@withContext ApiResult.Success(response, response.message)
                } else {
                    return@withContext ApiResult.Error(response.code, response.message ?: "Server returned error status")
                }
            } catch (e: Exception) {
                lastException = e
                val delayMs = delays[attempts]
                attempts++
                if (attempts < delays.size) {
                    delay(delayMs)
                }
            }
        }

        ApiResult.NetworkError(lastException ?: Exception("Network request failed after exponential retries"))
    }

    private fun executeHttpPost(urlString: String, jsonBody: String, redirectCount: Int = 0): ApiResponse {
        if (redirectCount > 5) {
            throw java.io.IOException("Too many HTTP redirects")
        }

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = NetworkConstants.CONNECT_TIMEOUT_MS
            connection.readTimeout = NetworkConstants.READ_TIMEOUT_MS
            connection.doOutput = true
            connection.doInput = true
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode

            // Handle Apps Script 302/307 Redirects manually if needed
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == 307
            ) {
                val newUrl = connection.getHeaderField("Location")
                if (!newUrl.isNullOrEmpty()) {
                    return executeHttpPost(newUrl, jsonBody, redirectCount + 1)
                }
            }

            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }
            return ApiResponse.fromJsonString(responseText)
        } finally {
            connection.disconnect()
        }
    }
}
