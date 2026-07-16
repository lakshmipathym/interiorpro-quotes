package com.example.utils

object ValidationManager {
    fun isValidPhone(phone: String): Boolean {
        val clean = phone.trim()
        return clean.length == 10 && clean.all { it.isDigit() }
    }
    
    fun isValidEmail(email: String): Boolean {
        val clean = email.trim()
        if (clean.isEmpty()) return false
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        return clean.matches(Regex(emailRegex))
    }
    
    fun isValidGstin(gstin: String): Boolean {
        val clean = gstin.trim().uppercase()
        if (clean.isEmpty()) return true
        return clean.length == 15 && clean.all { it.isLetterOrDigit() }
    }
}
