package com.example.engine

object TaxEngine {
    // Calculates GST amount based on taxable amount (subtotal - discount)
    fun calculateGstAmount(subtotal: Double, discount: Double, gstRate: Double): Double {
        val taxableAmount = maxOf(0.0, subtotal - discount)
        return taxableAmount * (gstRate / 100.0)
    }
}
