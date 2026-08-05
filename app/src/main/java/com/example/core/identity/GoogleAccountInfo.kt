package com.example.core.identity

data class GoogleAccountInfo(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val accountId: String
)
