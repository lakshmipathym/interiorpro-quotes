package com.example.domain.usecases

import com.example.domain.contracts.ItemCalculationEngine
import com.example.domain.contracts.QuotationCalculationEngine
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.RawItemInput
import com.example.domain.models.RawQuotationInput

class CalculateQuotationUseCase(
    private val itemEngine: ItemCalculationEngine,
    private val quotationEngine: QuotationCalculationEngine
) {
    fun execute(
        rawQuotationInput: RawQuotationInput,
        rawItems: List<RawItemInput>
    ): CalculatedQuotation {
        val calculatedItems = rawItems.map { itemEngine.calculateItem(it) }
        return quotationEngine.calculateQuotation(rawQuotationInput, calculatedItems)
    }

    fun previewItem(rawItemInput: RawItemInput) = itemEngine.calculateItem(rawItemInput)
}
