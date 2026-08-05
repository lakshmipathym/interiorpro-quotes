package com.example.core.network

import org.json.JSONObject

data class BaseApiRequest(
    val action: String,
    val deviceId: String,
    val installationId: String,
    val workspaceId: String,
    val deviceFingerprint: String,
    val appVersion: String,
    val email: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val nonce: String = java.util.UUID.randomUUID().toString(),
    val signature: String? = null,
    val licenseKey: String? = null,
    val customerName: String? = null,
    val phone: String? = null
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("action", action)
        json.put("deviceId", deviceId)
        json.put("installationId", installationId)
        json.put("workspaceId", workspaceId)
        json.put("deviceFingerprint", deviceFingerprint)
        json.put("appVersion", appVersion)
        email?.let { json.put("email", it) }
        json.put("timestampMs", timestampMs)
        json.put("nonce", nonce)
        signature?.let { json.put("signature", it) }
        licenseKey?.let { json.put("licenseKey", it) }
        customerName?.let { json.put("customerName", it) }
        phone?.let { json.put("phone", it) }
        return json.toString()
    }
}

data class ApiResponse(
    val status: String,
    val code: Int,
    val serverTime: String?,
    val apiVersion: String?,
    val message: String?,
    val data: JSONObject?
) {
    val isSuccess: Boolean
        get() = status.equals("SUCCESS", ignoreCase = true) || code in 200..299

    companion object {
        fun fromJsonString(jsonStr: String): ApiResponse {
            val json = JSONObject(jsonStr)
            val status = json.optString("status", "ERROR")
            val code = json.optInt("code", 400)
            val serverTime = json.optString("serverTime", null)
            val apiVersion = json.optString("apiVersion", null)
            val message = json.optString("message", null)
            val data = json.optJSONObject("data")
            return ApiResponse(status, code, serverTime, apiVersion, message, data)
        }
    }
}

data class CloudLicenseDetails(
    val licenseKey: String?,
    val plan: String,
    val status: String,
    val activatedOn: String?,
    val expiryDate: String?,
    val lastSeen: String?,
    val remainingDays: Long
)
