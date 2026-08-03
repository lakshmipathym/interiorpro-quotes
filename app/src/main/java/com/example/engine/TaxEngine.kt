package com.example.engine

object TaxEngine {
    // Calculates GST amount based on taxable amount (subtotal - discount)
    fun calculateGstAmount(subtotal: Double, discount: Double, gstRate: Double): Double {
        val taxableAmount = com.example.utils.CurrencyFormatter.normalizeCurrency(maxOf(0.0, subtotal - discount))
        val gst = taxableAmount * (gstRate / 100.0)
        return com.example.utils.CurrencyFormatter.normalizeCurrency(gst)
    }
}
