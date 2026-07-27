package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class QuotationCalculationEngineTest {
    
    private val delta = 0.0001
    
    @Test
    fun testSqFt() {
        // Test Case A
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "12 ft", 1.0, "Sq.Ft", "2 ft")
        assertEquals(120.0, qty, delta)
    }

    @Test
    fun testCuFt() {
        // Test Case B
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "12 ft", 1.0, "Cu.Ft", "2 ft")
        assertEquals(240.0, qty, delta)
    }

    @Test
    fun testRFt() {
        // Test Case C
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "0", 2.0, "R.Ft", "0")
        assertEquals(20.0, qty, delta)
    }

    @Test
    fun testMeter() {
        // Test Case D
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "0", 2.0, "Meter", "0")
        assertEquals(6.096, qty, delta)
    }

    @Test
    fun testSqM() {
        // Test Case E
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "12 ft", 1.0, "Sq.M", "0")
        assertEquals(11.1483648, qty, delta)
    }

    @Test
    fun testCuM() {
        // Test Case F
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "12 ft", 1.0, "Cu.M", "2 ft")
        assertEquals(6.79604318208, qty, delta)
    }

    @Test
    fun testNos() {
        val qty = QuotationCalculationEngine.calculateQuantity("10 ft", "12 ft", 5.0, "Nos", "2 ft")
        assertEquals(5.0, qty, delta)
    }

    @Test
    fun testNegativeValues() {
        val qty = QuotationCalculationEngine.calculateQuantity("-10 ft", "-12 ft", -5.0, "Sq.Ft", "-2 ft")
        assertEquals(0.0, qty, delta)
    }

    @Test
    fun testZeroValues() {
        val qty = QuotationCalculationEngine.calculateQuantity("0 ft", "0 ft", 0.0, "Sq.Ft", "0 ft")
        assertEquals(0.0, qty, delta)
    }

    @Test
    fun testInvalidInput() {
        val qty = QuotationCalculationEngine.calculateQuantity("abc", "xyz", 1.0, "Sq.Ft", "def")
        assertEquals(0.0, qty, delta)
    }

    @Test
    fun testMixedFeetInchDimensions() {
        val qty = QuotationCalculationEngine.calculateQuantity("10'6\"", "5'3\"", 1.0, "Sq.Ft", "0")
        // 10.5 * 5.25 = 55.125
        assertEquals(55.125, qty, delta)
    }
}
