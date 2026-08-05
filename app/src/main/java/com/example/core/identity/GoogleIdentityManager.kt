package com.example.core.identity

import android.content.Context
import android.content.SharedPreferences
import com.example.core.drive.GoogleSignInManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleIdentityManager(
    private val context: Context,
    private val googleSignInManager: GoogleSignInManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("google_identity_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EMAIL = "google_account_email"
        private const val KEY_DISPLAY_NAME = "google_display_name"
        private const val KEY_PHOTO_URL = "google_photo_url"
        private const val KEY_ACCOUNT_ID = "google_account_id"
    }

    private val _identityState = MutableStateFlow<GoogleIdentityState>(GoogleIdentityState.Disconnected)
    val identityState: StateFlow<GoogleIdentityState> = _identityState.asStateFlow()

    init {
        restoreAccountState()
    }

    private fun restoreAccountState() {
        val email = prefs.getString(KEY_EMAIL, null)
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null)
        val photoUrl = prefs.getString(KEY_PHOTO_URL, null)
        val accountId = prefs.getString(KEY_ACCOUNT_ID, null)

        if (email == "sandbox.user@interiorpro.tech") {
            clearAccountInfo()
            _identityState.value = GoogleIdentityState.Disconnected
            return
        }

        if (!email.isNullOrEmpty() && !accountId.isNullOrEmpty()) {
            val accountInfo = GoogleAccountInfo(
                email = email,
                displayName = displayName ?: email,
                photoUrl = photoUrl,
                accountId = accountId
            )
            _identityState.value = GoogleIdentityState.Connected(accountInfo)
        } else if (googleSignInManager.isUserSignedIn.value) {
            val signedInEmail = googleSignInManager.currentUserEmail.value
            val signedInName = googleSignInManager.currentUserDisplayName.value
            if (!signedInEmail.isNullOrEmpty()) {
                val accountInfo = GoogleAccountInfo(
                    email = signedInEmail,
                    displayName = signedInName ?: signedInEmail,
                    photoUrl = null,
                    accountId = signedInEmail
                )
                saveAccountInfo(accountInfo)
                _identityState.value = GoogleIdentityState.Connected(accountInfo)
            }
        }
    }

    fun getConnectedAccount(): GoogleAccountInfo? {
        val state = _identityState.value
        return if (state is GoogleIdentityState.Connected) state.account else null
    }

    suspend fun connectAccount(activityContext: Context): Boolean {
        _identityState.value = GoogleIdentityState.Connecting
        val success = googleSignInManager.signIn(activityContext)
        if (success) {
            val email = googleSignInManager.currentUserEmail.value ?: ""
            val name = googleSignInManager.currentUserDisplayName.value ?: email
            val accountInfo = GoogleAccountInfo(
                email = email,
                displayName = name,
                photoUrl = null,
                accountId = email
            )
            saveAccountInfo(accountInfo)
            _identityState.value = GoogleIdentityState.Connected(accountInfo)
            return true
        } else {
            _identityState.value = GoogleIdentityState.Error("Failed to connect Google Account.")
            return false
        }
    }

    suspend fun disconnectAccount(): Boolean {
        _identityState.value = GoogleIdentityState.Connecting
        val success = googleSignInManager.signOut()
        clearAccountInfo()
        _identityState.value = GoogleIdentityState.Disconnected
        return success
    }

    private fun saveAccountInfo(account: GoogleAccountInfo) {
        prefs.edit()
            .putString(KEY_EMAIL, account.email)
            .putString(KEY_DISPLAY_NAME, account.displayName)
            .putString(KEY_PHOTO_URL, account.photoUrl)
            .putString(KEY_ACCOUNT_ID, account.accountId)
            .apply()
    }

    private fun clearAccountInfo() {
        prefs.edit()
            .remove(KEY_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_PHOTO_URL)
            .remove(KEY_ACCOUNT_ID)
            .apply()
    }
}
