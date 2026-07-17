package com.example.core.drive

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * GoogleSignInManager manages active sessions, OAuth lifecycle, and user profile information.
 */
interface GoogleSignInManager {
    /**
     * Live observation flow representing active sign-in status.
     */
    val isUserSignedIn: StateFlow<Boolean>

    /**
     * Active Google account email.
     */
    val currentUserEmail: StateFlow<String?>

    /**
     * Active Google account display name.
     */
    val currentUserDisplayName: StateFlow<String?>

    /**
     * Triggers the interactive Google Sign-In using the modern Credential Manager API.
     */
    suspend fun signIn(activityContext: Context): Boolean

    /**
     * Attempts to silently authenticate the user (silent sign-in/auto-select).
     */
    suspend fun silentSignIn(): Boolean

    /**
     * Signs out the current session and clears credentials.
     */
    suspend fun signOut(): Boolean

    /**
     * Returns the active OAuth2 access token for raw REST calls if required.
     */
    suspend fun getAccessToken(): String?
}
