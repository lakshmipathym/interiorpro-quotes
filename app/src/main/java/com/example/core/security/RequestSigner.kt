package com.example.core.security

import android.content.Context
import com.example.core.network.BaseApiRequest
import com.example.core.network.NetworkConstants
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object RequestSigner {

    fun signRequest(
        request: BaseApiRequest,
        context: Context? = null,
        overrideSecret: String? = null
    ): BaseApiRequest {
        val secretKey = overrideSecret ?: NetworkConstants.getSecretKey(context)
        val payloadToSign = "${request.action}|${request.deviceId}|${request.workspaceId}|${request.deviceFingerprint}|${request.timestampMs}|${request.nonce}"
        val signature = computeHmacSha256(payloadToSign, secretKey)
        return request.copy(signature = signature)
    }

    fun computeHmacSha256(data: String, secret: String): String {
        return try {
            val hmacKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(hmacKey)
            val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
