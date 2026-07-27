package com.example.engine

import com.example.data.QuotationItem
import java.util.Locale

object QuotationCalculationEngine {

    fun parseDimensionToFeet(dimStr: String): Double {
        val clean = dimStr.lowercase(Locale.US).trim()
        if (clean.isEmpty() || clean == "null" || clean == "0" || clean == "0.0") {
            return 0.0
        }

        if (clean.contains("'")) {
            val parts = clean.split("'")
            val feetPart = parts[0].trim().toDoubleOrNull() ?: 0.0
            var inchPartStr = if (parts.size > 1) parts[1] else "0"
            inchPartStr = inchPartStr.replace("\"", "").replace("in", "").replace("inch", "").trim()
            val inchPart = inchPartStr.toDoubleOrNull() ?: 0.0
            return feetPart + (inchPart / 12.0)
        }

        if (clean.contains("\"") || clean.contains("in") || clean.contains("inch")) {
            val inchPartStr = clean.replace("\"", "").replace("in", "").replace("inch", "").trim()
            val inchPart = inchPartStr.toDoubleOrNull() ?: 0.0
            return inchPart / 12.0
        }

        val pureNumber = clean.toDoubleOrNull()
        if (pureNumber != null) {
            return pureNumber
        }

        val feetPartStr = clean.replace("ft", "").replace("feet", "").trim()
        return feetPartStr.toDoubleOrNull() ?: 0.0
    }

    fun calculateQuantity(width: String, height: String, qtyCount: Double, unit: String, depth: String = "0"): Double {
        val wFeet = maxOf(0.0, parseDimensionToFeet(width))
        val hFeet = maxOf(0.0, parseDimensionToFeet(height))
        val dFeet = maxOf(0.0, parseDimensionToFeet(depth))
        val safeQty = maxOf(0.0, qtyCount)
        val uLower = unit.trim().lowercase(Locale.US)

        return when {
            uLower == "cu.m" || uLower.contains("cu.m") || uLower.contains("cum") || uLower.contains("cubic meter") || uLower.contains("cubic mtr") -> {
                wFeet * hFeet * dFeet * 0.028316846592 * safeQty
            }
            uLower == "cu.ft" || uLower.contains("cu.ft") || uLower.contains("cuft") || uLower.contains("cubic feet") || uLower.contains("cft") -> {
                wFeet * hFeet * dFeet * safeQty
            }
            uLower == "sq.m" || uLower.contains("sq.m") || uLower.contains("sqm") || uLower.contains("square meter") || uLower.contains("square mtr") -> {
                wFeet * hFeet * 0.09290304 * safeQty
            }
            uLower == "sq.ft" || uLower.contains("sq.ft") || uLower.contains("sqft") || uLower == "sft" || uLower.contains("square feet") || uLower.contains("sq") -> {
                wFeet * hFeet * safeQty
            }
            uLower == "meter" || uLower.contains("meter") || uLower.contains("mtr") || uLower == "r.m" || uLower == "rm" -> {
                wFeet * 0.3048 * safeQty
            }
            uLower == "running feet" || uLower.contains("run") || uLower.contains("rft") || uLower.contains("r.ft") || uLower == "r.f" -> {
                wFeet * safeQty
            }
            uLower == "lumpsum" || uLower.contains("lump sum") || uLower == "l.s" || uLower == "ls" -> {
                safeQty
            }
            else -> {
                safeQty
            }
        }.let {
            if (it.isNaN() || it.isInfinite()) 0.0 else it
        }
    }

    fun calculateItemAmount(width: String, height: String, qtyCount: Double, unit: String, rate: Double, depth: String = "0"): Double {
        val safeRate = maxOf(0.0, rate)
        val amount = calculateQuantity(width, height, qtyCount, unit, depth) * safeRate
        return if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
    }

    fun calculateSubtotal(items: List<QuotationItem>): Double {
        val sum = items.sumOf { it.amount }
        return Math.round(sum * 100.0) / 100.0
    }
        
    fun calculateGrandTotal(
        subtotal: Double, 
        discount: Double, 
        gstAmount: Double, 
        transport: Double = 0.0,
        installation: Double = 0.0,
        extraCharges: Double = 0.0,
        roundOff: Double = 0.0
    ): Double {
        val taxable = maxOf(0.0, subtotal - discount)
        val grandTotalRaw = taxable + gstAmount + transport + installation + extraCharges
        // Return exact decimal to avoid ₹0 on small amounts
        return Math.round((grandTotalRaw + roundOff) * 100.0) / 100.0
    }
}
