package com.example.pdf
import com.example.data.CompanyProfile

val CompanyProfile.canonicalName: String
    get() = companyName.trim().ifBlank { "Company Name" }
