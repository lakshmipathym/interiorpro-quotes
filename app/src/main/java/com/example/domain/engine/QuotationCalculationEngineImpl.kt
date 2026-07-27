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
        
        val subtotal = calculatedItems.sumOf { it.itemAmount }
        
        val discount = if (input.discount < 0.0) 0.0 else input.discount
        val taxableAmount = max(0.0, subtotal - discount)
        
        val gstRate = if (input.gstRate < 0.0) 0.0 else input.gstRate
        val gstAmount = taxableAmount * (gstRate / 100.0)
        
        val grandTotal = taxableAmount + gstAmount + 
                         input.transport + 
                         input.installation + 
                         input.extraCharges + 
                         input.roundOff
                         
        val advance = input.advance
        val balanceDue = max(0.0, grandTotal - advance)
        
        // Business Rule: PDF generator rounded grand total to 2 decimals before converting
        val normalizedFinalGrandTotal = round(grandTotal * 100.0) / 100.0
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
