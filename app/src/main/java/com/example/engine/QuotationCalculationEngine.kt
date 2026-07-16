package com.example.engine

import com.example.data.QuotationItem

object QuotationCalculationEngine {
    fun calculateSubtotal(items: List<QuotationItem>): Double {
        return items.sumOf { it.amount }
    }
    
    fun calculateGrandTotal(subtotal: Double, discount: Double, gstAmount: Double): Double {
        return maxOf(0.0, subtotal - discount + gstAmount)
    }
}
