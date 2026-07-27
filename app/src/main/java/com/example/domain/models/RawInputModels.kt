package com.example.domain.models

data class RawItemInput(
    val itemName: String = "",
    val description: String = "",
    val material: String = "",
    val finish: String = "",
    val width: String = "0",
    val height: String = "0",
    val depth: String = "0",
    val quantity: Double = 1.0,
    val unit: String = "Nos",
    val rate: Double = 0.0
)

data class RawQuotationInput(
    val discount: Double = 0.0,
    val gstRate: Double = 0.0,
    val transport: Double = 0.0,
    val installation: Double = 0.0,
    val extraCharges: Double = 0.0,
    val roundOff: Double = 0.0,
    val advance: Double = 0.0
)
