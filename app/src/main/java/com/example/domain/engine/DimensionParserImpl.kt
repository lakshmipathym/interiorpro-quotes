package com.example.domain.engine

import com.example.domain.contracts.DimensionParser

class DimensionParserImpl : DimensionParser {
    override fun parseToFeet(dimension: String): Double {
        if (dimension.isBlank()) return 0.0
        val sanitized = dimension.trim().lowercase()
        if (sanitized == "0") return 0.0

        try {
            // First try simple number parse (e.g., "10.5")
            val simpleParse = sanitized.toDoubleOrNull()
            if (simpleParse != null) return simpleParse

            // Handle "10 ft" or "10ft"
            if (sanitized.contains("ft") || sanitized.contains("feet")) {
                val numPart = sanitized.replace("ft", "").replace("feet", "").trim()
                return numPart.toDoubleOrNull() ?: 0.0
            }

            // Handle "10' 6\"" or "10'"
            if (sanitized.contains("'") || sanitized.contains("\"")) {
                val parts = sanitized.split("'")
                val feet = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
                val inchesPart = parts.getOrNull(1)?.replace("\"", "")?.trim()
                val inches = inchesPart?.toDoubleOrNull() ?: 0.0
                return feet + (inches / 12.0)
            }
        } catch (e: Exception) {
            // Fallback
        }
        return 0.0
    }
}
