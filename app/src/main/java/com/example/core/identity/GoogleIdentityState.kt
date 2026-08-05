package com.example.core.identity

sealed class GoogleIdentityState {
    object Disconnected : GoogleIdentityState()
    object Connecting : GoogleIdentityState()
    data class Connected(val account: GoogleAccountInfo) : GoogleIdentityState()
    data class Error(val message: String) : GoogleIdentityState()
}
