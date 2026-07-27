package com.example.domain.engine

import com.example.domain.models.CalculatedItem
import com.example.domain.models.RawItemInput
import com.example.domain.models.RawQuotationInput
import com.example.domain.models.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuotationCalculationEngineImplTest {

    private val amountInWordsConverter = AmountInWordsConverterImpl()
    private val engine = QuotationCalculationEngineImpl(amountInWordsConverter)

    @Test
    fun testSubtotalCalculation() {
        val input = RawQuotationInput(
            discount = 0.0,
            gstRate = 0.0,
            transport = 0.0,
            installation = 0.0,
            extraCharges = 0.0,
            roundOff = 0.0,
            advance = 0.0
        )
        val items = listOf(
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 10.0, parsedHeight = 10.0, parsedDepth = 0.0, parsedUnit = UnitType.SQ_FT, billableQuantity = 100.0, itemAmount = 1500.0),
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 0.0, parsedHeight = 0.0, parsedDepth = 0.0, parsedUnit = UnitType.LUMPSUM, billableQuantity = 1.0, itemAmount = 500.0)
        )
        val result = engine.calculateQuotation(input, items)
        assertEquals(2000.0, result.subtotal, 0.001)
        assertEquals(2000.0, result.taxableAmount, 0.001)
        assertEquals(2000.0, result.grandTotal, 0.001)
        assertEquals(2000.0, result.balanceDue, 0.001)
    }

    @Test
    fun testDiscountAndTaxableAmount() {
        val input = RawQuotationInput(
            discount = 200.0,
            gstRate = 0.0,
            transport = 0.0,
            installation = 0.0,
            extraCharges = 0.0,
            roundOff = 0.0,
            advance = 0.0
        )
        val items = listOf(
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 0.0, parsedHeight = 0.0, parsedDepth = 0.0, parsedUnit = UnitType.LUMPSUM, billableQuantity = 1.0, itemAmount = 1000.0)
        )
        val result = engine.calculateQuotation(input, items)
        assertEquals(1000.0, result.subtotal, 0.001)
        assertEquals(800.0, result.taxableAmount, 0.001)
        assertEquals(800.0, result.grandTotal, 0.001)
    }

    @Test
    fun testGSTCalculation() {
        val input = RawQuotationInput(
            discount = 0.0,
            gstRate = 18.0,
            transport = 0.0,
            installation = 0.0,
            extraCharges = 0.0,
            roundOff = 0.0,
            advance = 0.0
        )
        val items = listOf(
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 0.0, parsedHeight = 0.0, parsedDepth = 0.0, parsedUnit = UnitType.LUMPSUM, billableQuantity = 1.0, itemAmount = 1000.0)
        )
        val result = engine.calculateQuotation(input, items)
        assertEquals(1000.0, result.taxableAmount, 0.001)
        assertEquals(180.0, result.gstAmount, 0.001)
        assertEquals(1180.0, result.grandTotal, 0.001)
    }

    @Test
    fun testOtherChargesAndRoundOff() {
        val input = RawQuotationInput(
            discount = 0.0,
            gstRate = 0.0,
            transport = 50.0,
            installation = 100.0,
            extraCharges = 20.0,
            roundOff = 0.5,
            advance = 500.0
        )
        val items = listOf(
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 0.0, parsedHeight = 0.0, parsedDepth = 0.0, parsedUnit = UnitType.LUMPSUM, billableQuantity = 1.0, itemAmount = 1000.0)
        )
        val result = engine.calculateQuotation(input, items)
        assertEquals(1000.0, result.taxableAmount, 0.001)
        assertEquals(0.0, result.gstAmount, 0.001)
        // 1000 + 50 + 100 + 20 + 0.5 = 1170.5
        assertEquals(1170.5, result.grandTotal, 0.001)
        // 1170.5 - 500 = 670.5
        assertEquals(670.5, result.balanceDue, 0.001)
    }

    @Test
    fun testAmountInWords() {
        val input = RawQuotationInput(
            discount = 0.0,
            gstRate = 0.0,
            transport = 0.0,
            installation = 0.0,
            extraCharges = 0.0,
            roundOff = 0.0,
            advance = 0.0
        )
        val items = listOf(
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 0.0, parsedHeight = 0.0, parsedDepth = 0.0, parsedUnit = UnitType.LUMPSUM, billableQuantity = 1.0, itemAmount = 1000.0)
        )
        val result = engine.calculateQuotation(input, items)
        assertEquals("One Thousand Only", result.amountInWords)
    }

    @Test
    fun testNegativeValuesValidation() {
        val input = RawQuotationInput(
            discount = -100.0,
            gstRate = -18.0,
            transport = 0.0,
            installation = 0.0,
            extraCharges = 0.0,
            roundOff = 0.0,
            advance = 0.0
        )
        val items = listOf(
            CalculatedItem(rawInput = RawItemInput(), parsedWidth = 0.0, parsedHeight = 0.0, parsedDepth = 0.0, parsedUnit = UnitType.LUMPSUM, billableQuantity = 1.0, itemAmount = 1000.0)
        )
        val result = engine.calculateQuotation(input, items)
        assertEquals(1000.0, result.subtotal, 0.001)
        // Negative discount should be ignored (or treated as 0)
        assertEquals(1000.0, result.taxableAmount, 0.001)
        // Negative GST should be ignored
        assertEquals(0.0, result.gstAmount, 0.001)
        assertEquals(1000.0, result.grandTotal, 0.001)
    }
}
