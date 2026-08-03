package com.example.domain.engine

import com.example.domain.contracts.AmountInWordsConverter
import com.example.domain.contracts.QuotationCalculationEngine
import com.example.domain.models.CalculatedItem
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.RawQuotationInput
import kotlin.math.max
import kotlin.math.round

class QuotationCalculationEngineImpl(
    private val amountInWordsConverter: AmountInWordsConverter? = null
) : QuotationCalculationEngine {

    override fun calculateQuotation(
        input: RawQuotationInput,
        calculatedItems: List<CalculatedItem>
    ): CalculatedQuotation {
        
        val subtotal = com.example.utils.CurrencyFormatter.normalizeCurrency(calculatedItems.sumOf { it.itemAmount })
        
        val discount = com.example.utils.CurrencyFormatter.normalizeCurrency(if (input.discount < 0.0) 0.0 else input.discount)
        val taxableAmount = com.example.utils.CurrencyFormatter.normalizeCurrency(max(0.0, subtotal - discount))
        
        val gstRate = if (input.gstRate < 0.0) 0.0 else input.gstRate
        val gstAmount = com.example.utils.CurrencyFormatter.normalizeCurrency(taxableAmount * (gstRate / 100.0))
        
        val grandTotalRaw = taxableAmount + gstAmount + 
                         com.example.utils.CurrencyFormatter.normalizeCurrency(input.transport) + 
                         com.example.utils.CurrencyFormatter.normalizeCurrency(input.installation) + 
                         com.example.utils.CurrencyFormatter.normalizeCurrency(input.extraCharges) + 
                         com.example.utils.CurrencyFormatter.normalizeCurrency(input.roundOff)
        val grandTotal = com.example.utils.CurrencyFormatter.normalizeCurrency(grandTotalRaw)
                         
        val advance = com.example.utils.CurrencyFormatter.normalizeCurrency(input.advance)
        val balanceDue = com.example.utils.CurrencyFormatter.normalizeCurrency(max(0.0, grandTotal - advance))
        
        // Business Rule: PDF generator rounded grand total to 2 decimals before converting
        val normalizedFinalGrandTotal = grandTotal
        val amountInWords = amountInWordsConverter?.convertToWords(normalizedFinalGrandTotal) ?: ""

        return CalculatedQuotation(
            items = calculatedItems,
            subtotal = subtotal,
            taxableAmount = taxableAmount,
            gstAmount = gstAmount,
            grandTotal = grandTotal,
            balanceDue = balanceDue,
            amountInWords = amountInWords
        )
    }
}
