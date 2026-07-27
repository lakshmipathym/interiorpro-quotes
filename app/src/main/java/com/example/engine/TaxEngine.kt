package com.example.engine

object TaxEngine {
    // Calculates GST amount based on taxable amount (subtotal - discount)
    fun calculateGstAmount(subtotal: Double, discount: Double, gstRate: Double): Double {
        val taxableAmount = maxOf(0.0, subtotal - discount)
        val gst = taxableAmount * (gstRate / 100.0)
        return Math.round(gst * 100.0) / 100.0
    }
}
