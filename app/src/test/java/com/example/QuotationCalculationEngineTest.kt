package com.example

import com.example.engine.QuotationCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class QuotationCalculationEngineTest {
    
    @Test
    fun testSqFtCalculation() {
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 2.0, "Sq.Ft")
        // 10' * 12' * 2 = 120 * 2 = 240
        assertEquals(240.0, qty, 0.001)
    }

    @Test
    fun testSqMCalculation() {
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 2.0, "Sq.M")
        // 10 * 12 * 0.09290304 * 2 = 22.2967296
        assertEquals(22.2967296, qty, 0.001)
    }

    @Test
    fun testRftCalculation() {
        // running feet only uses width
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 2.0, "R.Ft")
        // 10 * 2 = 20
        assertEquals(20.0, qty, 0.001)
    }
    
    @Test
    fun testMeterCalculation() {
        // meter only uses width * 0.3048
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 2.0, "Meter")
        // 10 * 0.3048 * 2 = 6.096
        assertEquals(6.096, qty, 0.001)
    }

    @Test
    fun testCuFtCalculation() {
        // depth is 5 feet
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 2.0, "Cu.Ft", "5")
        // 10 * 12 * 5 * 2 = 1200
        assertEquals(1200.0, qty, 0.001)
    }

    @Test
    fun testCuMCalculation() {
        // depth is 5 feet
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 2.0, "Cu.M", "5")
        // 10 * 12 * 5 * 0.028316846592 * 2 = 33.9802159104
        assertEquals(33.98021591, qty, 0.001)
    }
    
    @Test
    fun testNosCalculation() {
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 5.0, "Nos")
        assertEquals(5.0, qty, 0.001)
    }

    @Test
    fun testLumpsumCalculation() {
        val qty = QuotationCalculationEngine.calculateQuantity("10", "12", 1.0, "Lumpsum")
        assertEquals(1.0, qty, 0.001)
    }

    @Test
    fun testNegativeDimensionsHandledSafely() {
        val qty = QuotationCalculationEngine.calculateQuantity("-10", "12", 2.0, "Sq.Ft")
        // -10 parsed -> might result in negative if parseDimensionToFeet doesn't block it,
        // but we added maxOf(0.0) so it should be 0.
        assertEquals(0.0, qty, 0.001)
        
        val qtyNos = QuotationCalculationEngine.calculateQuantity("10", "12", -2.0, "Nos")
        assertEquals(0.0, qtyNos, 0.001)
    }
    
    @Test
    fun testInchConversion() {
        val qty = QuotationCalculationEngine.calculateQuantity("10\"", "12\"", 1.0, "Sq.Ft")
        // 10/12 * 12/12 = 0.8333333 * 1 = 0.8333333
        assertEquals(0.8333333, qty, 0.001)

        val qtyMixed = QuotationCalculationEngine.calculateQuantity("10' 6\"", "2' 6\"", 1.0, "Sq.Ft")
        // 10.5 * 2.5 = 26.25
        assertEquals(26.25, qtyMixed, 0.001)
        
        val qtyRegression2 = QuotationCalculationEngine.calculateQuantity("120\"", "144\"", 1.0, "Sq.Ft")
        assertEquals(120.0, qtyRegression2, 0.001)
        
        val qtyRegression4 = QuotationCalculationEngine.calculateQuantity("120\"", "144\"", 1.0, "Cu.Ft", "18\"")
        assertEquals(180.0, qtyRegression4, 0.001) // 10 * 12 * 1.5 = 180
    }

    @Test
    fun testEdgeCases() {
        // Invalid numbers -> 0
        val qty = QuotationCalculationEngine.calculateQuantity("abc", "def", 2.0, "Sq.Ft")
        assertEquals(0.0, qty, 0.001)
    }
}
