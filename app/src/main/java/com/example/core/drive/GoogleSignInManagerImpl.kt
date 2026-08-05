package com.example.core.drive

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class GoogleSignInManagerImpl(private val context: Context) : GoogleSignInManager {

    companion object {
        private const val TAG = "GoogleSignInManager"
        // Production Web Client ID configured for Server Client ID
        private const val DEFAULT_CLIENT_ID = "485179806050-8pt0kacg2k65asrinsvrv1p76lfi1q17.apps.googleusercontent.com"
    }

    private val credentialManager by lazy { CredentialManager.create(context) }
    private val prefs = context.getSharedPreferences("interiorpro_auth_prefs", Context.MODE_PRIVATE)

    private val _isUserSignedIn = MutableStateFlow(false)
    override val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    override val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _currentUserDisplayName = MutableStateFlow<String?>(null)
    override val currentUserDisplayName: StateFlow<String?> = _currentUserDisplayName.asStateFlow()

    private var activeIdToken: String? = null

    init {
        // Hydrate authentication state from persistent local storage for single-source offline-first state consistency
        val savedEmail = prefs.getString("auth_user_email", null)
        val savedName = prefs.getString("auth_user_name", null)
        val savedToken = prefs.getString("auth_id_token", null)
        if (!savedEmail.isNullOrEmpty() && savedEmail != "sandbox.user@interiorpro.tech") {
            _currentUserEmail.value = savedEmail
            _currentUserDisplayName.value = savedName
            activeIdToken = savedToken
            _isUserSignedIn.value = true
            Log.i(TAG, "Restored active user session for: $savedEmail")
        } else if (savedEmail == "sandbox.user@interiorpro.tech") {
            prefs.edit()
                .remove("auth_user_email")
                .remove("auth_user_name")
                .remove("auth_id_token")
                .apply()
        }
    }

    private fun logDetailedException(action: String, e: Throwable) {
        Log.e(TAG, "======== BEGIN DETAILED EXCEPTION LOG ($action) ========")
        Log.e(TAG, "Exception Class: ${e.javaClass.name}")
        Log.e(TAG, "Exception Message: ${e.message}")
        if (e is GetCredentialException) {
            Log.e(TAG, "GetCredentialException type: ${e.type}")
        }

        val cause = e.cause
        if (cause != null) {
            Log.e(TAG, "Cause Class: ${cause.javaClass.name}")
            Log.e(TAG, "Cause Message: ${cause.message}")
        }

        val apiException = (e as? ApiException) ?: (cause as? ApiException)

        if (apiException != null) {
            val statusCode = apiException.statusCode
            val statusMessage = apiException.statusMessage
            val statusCodeString = CommonStatusCodes.getStatusCodeString(statusCode)
            Log.e(TAG, "ApiException Details:")
            Log.e(TAG, "  - statusCode: $statusCode")
            Log.e(TAG, "  - statusCode Name: $statusCodeString")
            Log.e(TAG, "  - statusMessage: $statusMessage")
        } else {
            Log.e(TAG, "No ApiException found in exception hierarchy.")
        }

        Log.e(TAG, "Full Exception Stack Trace:", e)
        if (cause != null && cause != e) {
            Log.e(TAG, "Full Cause Stack Trace:", cause)
        }
        Log.e(TAG, "======== END DETAILED EXCEPTION LOG ($action) ========")
    }

    private fun extractApiErrorMessage(e: Throwable): String {
        val cause = e.cause
        val apiEx = (e as? ApiException) ?: (cause as? ApiException)
        return if (apiEx != null) {
            "\n[ApiException code: ${apiEx.statusCode} (${CommonStatusCodes.getStatusCodeString(apiEx.statusCode)}), msg: ${apiEx.statusMessage}]"
        } else {
            cause?.let { "\n[Cause: ${it.javaClass.simpleName}: ${it.message}]" } ?: ""
        }
    }

    override suspend fun signIn(activityContext: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initiating Google Sign-In request with serverClientId=$DEFAULT_CLIENT_ID, filterByAuthorizedAccounts=false")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(DEFAULT_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.i(TAG, "Calling CredentialManager.getCredential with GetGoogleIdOption")
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            Log.i(TAG, "Credential received successfully: type=${credential.type}")

            if (credential is GoogleIdTokenCredential) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.i(TAG, "GoogleIdTokenCredential extracted for user: ${googleIdTokenCredential.id}")
                
                _currentUserEmail.value = googleIdTokenCredential.id
                _currentUserDisplayName.value = googleIdTokenCredential.displayName
                activeIdToken = googleIdTokenCredential.idToken
                _isUserSignedIn.value = true

                prefs.edit().apply {
                    putString("auth_user_email", googleIdTokenCredential.id)
                    putString("auth_user_name", googleIdTokenCredential.displayName)
                    putString("auth_id_token", googleIdTokenCredential.idToken)
                    apply()
                }
                return@withContext true
            } else {
                Log.w(TAG, "Received credential type is not GoogleIdTokenCredential: ${credential.type}")
                return@withContext false
            }
        } catch (e: NoCredentialException) {
            logDetailedException("signIn - NoCredentialException", e)
            val extraInfo = extractApiErrorMessage(e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activityContext, "No Google accounts found or sign-in cancelled: ${e.message}$extraInfo", Toast.LENGTH_LONG).show()
            }
            return@withContext false
        } catch (e: GetCredentialException) {
            logDetailedException("signIn - GetCredentialException", e)
            val extraInfo = extractApiErrorMessage(e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activityContext, "Sign-in error (${e.javaClass.name}): ${e.message}$extraInfo", Toast.LENGTH_LONG).show()
            }
            return@withContext false
        } catch (e: Exception) {
            logDetailedException("signIn - Exception", e)
            val extraInfo = extractApiErrorMessage(e)
            withContext(Dispatchers.Main) {
                Toast.makeText(activityContext, "Sign-in exception (${e.javaClass.name}): ${e.message}$extraInfo", Toast.LENGTH_LONG).show()
            }
            return@withContext false
        }
    }

    override suspend fun silentSignIn(): Boolean = withContext(Dispatchers.IO) {
        if (_isUserSignedIn.value && !_currentUserEmail.value.isNullOrEmpty() && _currentUserEmail.value != "sandbox.user@interiorpro.tech") {
            Log.i(TAG, "silentSignIn: Active session already present for ${_currentUserEmail.value}")
            return@withContext true
        }
        try {
            Log.i(TAG, "Attempting silent Google Sign-In with filterByAuthorizedAccounts=true")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(DEFAULT_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            Log.i(TAG, "silentSignIn credential received: type=${credential.type}")

            if (credential is GoogleIdTokenCredential) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Log.i(TAG, "silentSignIn successful for user: ${googleIdTokenCredential.id}")
                
                _currentUserEmail.value = googleIdTokenCredential.id
                _currentUserDisplayName.value = googleIdTokenCredential.displayName
                activeIdToken = googleIdTokenCredential.idToken
                _isUserSignedIn.value = true

                prefs.edit().apply {
                    putString("auth_user_email", googleIdTokenCredential.id)
                    putString("auth_user_name", googleIdTokenCredential.displayName)
                    putString("auth_id_token", googleIdTokenCredential.idToken)
                    apply()
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: NoCredentialException) {
            Log.i(TAG, "silentSignIn: No saved authorized credential found for silent sign-in: ${e.message}")
            return@withContext false
        } catch (e: GetCredentialException) {
            Log.w(TAG, "silentSignIn: Credential Manager silent sign-in failed (${e.javaClass.name}: ${e.message})")
            return@withContext false
        } catch (e: Exception) {
            Log.w(TAG, "silentSignIn exception: ${e.javaClass.name}: ${e.message}")
            return@withContext false
        }
    }

    override suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Signing out user: ${_currentUserEmail.value}")
            if (android.os.Build.FINGERPRINT != "robolectric") {
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
            }
            _currentUserEmail.value = null
            _currentUserDisplayName.value = null
            activeIdToken = null
            _isUserSignedIn.value = false

            prefs.edit().apply {
                remove("auth_user_email")
                remove("auth_user_name")
                remove("auth_id_token")
                apply()
            }
            Log.i(TAG, "User signed out successfully")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun getAccessToken(): String? {
        return activeIdToken ?: prefs.getString("auth_id_token", null)
    }
}
