package com.example.domain.models

data class CalculatedItem(
    val rawInput: RawItemInput,
    val parsedWidth: Double,
    val parsedHeight: Double,
    val parsedDepth: Double,
    val parsedUnit: UnitType,
    val billableQuantity: Double,
    val itemAmount: Double
)

data class CalculatedQuotation(
    val items: List<CalculatedItem>,
    val subtotal: Double,
    val taxableAmount: Double,
    val gstAmount: Double,
    val grandTotal: Double,
    val balanceDue: Double,
    val amountInWords: String = ""
)
