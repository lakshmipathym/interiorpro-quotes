package com.example.domain.contracts

import com.example.domain.models.CalculatedItem
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.RawItemInput
import com.example.domain.models.RawQuotationInput

interface DimensionParser {
    fun parseToFeet(dimension: String): Double
}

interface ItemCalculationEngine {
    fun calculateItem(input: RawItemInput): CalculatedItem
}

interface AmountInWordsConverter {
    fun convertToWords(amount: Double): String
}

interface QuotationCalculationEngine {
    fun calculateQuotation(input: RawQuotationInput, calculatedItems: List<CalculatedItem>): CalculatedQuotation
}
