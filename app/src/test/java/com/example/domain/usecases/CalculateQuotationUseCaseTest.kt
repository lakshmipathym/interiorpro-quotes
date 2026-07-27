package com.example.domain.usecases

import com.example.domain.contracts.ItemCalculationEngine
import com.example.domain.contracts.QuotationCalculationEngine
import com.example.domain.models.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateQuotationUseCaseTest {

    private class DummyItemCalculationEngine : ItemCalculationEngine {
        override fun calculateItem(input: RawItemInput): CalculatedItem {
            return CalculatedItem(
                rawInput = input,
                parsedWidth = 10.0,
                parsedHeight = 10.0,
                parsedDepth = 0.0,
                parsedUnit = UnitType.SQ_FT,
                billableQuantity = 100.0,
                itemAmount = 100.0 * input.rate
            )
        }
    }

    private class DummyQuotationCalculationEngine : QuotationCalculationEngine {
        override fun calculateQuotation(
            input: RawQuotationInput,
            calculatedItems: List<CalculatedItem>
        ): CalculatedQuotation {
            val subtotal = calculatedItems.sumOf { it.itemAmount }
            val taxableAmount = subtotal - input.discount
            val gstAmount = taxableAmount * (input.gstRate / 100.0)
            val grandTotal = taxableAmount + gstAmount + input.transport + input.installation + input.extraCharges + input.roundOff
            val balanceDue = grandTotal - input.advance
            
            return CalculatedQuotation(
                items = calculatedItems,
                subtotal = subtotal,
                taxableAmount = taxableAmount,
                gstAmount = gstAmount,
                grandTotal = grandTotal,
                balanceDue = balanceDue,
                amountInWords = "DUMMY WORDS"
            )
        }
    }

    private lateinit var calculateQuotationUseCase: CalculateQuotationUseCase

    @Before
    fun setup() {
        val itemEngine = DummyItemCalculationEngine()
        val quotationEngine = DummyQuotationCalculationEngine()
        calculateQuotationUseCase = CalculateQuotationUseCase(itemEngine, quotationEngine)
    }

    @Test
    fun `test valid raw quotation input returns calculated quotation successfully`() {
        val rawQuotationInput = RawQuotationInput(
            discount = 0.0,
            gstRate = 0.0,
            transport = 0.0,
            installation = 0.0,
            extraCharges = 0.0,
            roundOff = 0.0,
            advance = 0.0
        )
        val rawItems = listOf(
            RawItemInput(rate = 10.0),
            RawItemInput(rate = 20.0)
        )

        val result = calculateQuotationUseCase.execute(rawQuotationInput, rawItems)

        assertEquals(2, result.items.size)
        assertEquals(3000.0, result.subtotal, 0.0) // 100 * 10 + 100 * 20
        assertEquals(3000.0, result.grandTotal, 0.0)
    }

    @Test
    fun `test multiple quotation items preserve all calculated item results`() {
        val rawQuotationInput = RawQuotationInput()
        val rawItems = listOf(
            RawItemInput(itemName = "Item 1", rate = 10.0),
            RawItemInput(itemName = "Item 2", rate = 20.0)
        )

        val result = calculateQuotationUseCase.execute(rawQuotationInput, rawItems)

        assertEquals("Item 1", result.items[0].rawInput.itemName)
        assertEquals("Item 2", result.items[1].rawInput.itemName)
    }

    @Test
    fun `test billable quantity delegates to PHASE 5A engine without recalculating`() {
        val rawQuotationInput = RawQuotationInput()
        val rawItems = listOf(RawItemInput(rate = 15.0))

        val result = calculateQuotationUseCase.execute(rawQuotationInput, rawItems)

        // The DummyItemCalculationEngine hardcodes billableQuantity to 100.0
        assertEquals(100.0, result.items[0].billableQuantity, 0.0)
        assertEquals(1500.0, result.items[0].itemAmount, 0.0)
    }

    @Test
    fun `test financial values return PHASE 5B financial result exactly`() {
        val rawQuotationInput = RawQuotationInput(
            discount = 100.0,
            gstRate = 18.0, // (1500 - 100) * 0.18 = 1400 * 0.18 = 252
            transport = 50.0,
            installation = 100.0,
            extraCharges = 20.0,
            roundOff = 0.5,
            advance = 500.0
        )
        val rawItems = listOf(RawItemInput(rate = 15.0)) // Amount = 1500

        val result = calculateQuotationUseCase.execute(rawQuotationInput, rawItems)

        assertEquals(1500.0, result.subtotal, 0.0)
        assertEquals(1400.0, result.taxableAmount, 0.0)
        assertEquals(252.0, result.gstAmount, 0.0)
        assertEquals(1822.5, result.grandTotal, 0.0) // 1400 + 252 + 50 + 100 + 20 + 0.5
        assertEquals(1322.5, result.balanceDue, 0.0) // 1822.5 - 500
    }
}
