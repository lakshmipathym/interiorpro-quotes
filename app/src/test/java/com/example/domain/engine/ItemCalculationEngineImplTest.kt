package com.example.domain.engine

import com.example.domain.models.RawItemInput
import com.example.domain.models.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemCalculationEngineImplTest {

    private val parser = DimensionParserImpl()
    private val engine = ItemCalculationEngineImpl(parser)

    @Test
    fun testSqFtCalculation() {
        val input = RawItemInput(width = "10", height = "10", depth = "10", quantity = 2.0, unit = "Sq.Ft", rate = 50.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.SQ_FT, result.parsedUnit)
        assertEquals(10.0, result.parsedWidth, 0.001)
        assertEquals(10.0, result.parsedHeight, 0.001)
        assertEquals(200.0, result.billableQuantity, 0.001) // 10 * 10 * 2
        assertEquals(10000.0, result.itemAmount, 0.001) // 200 * 50
    }

    @Test
    fun testCuFtCalculation() {
        val input = RawItemInput(width = "10", height = "10", depth = "2", quantity = 1.0, unit = "Cu.Ft", rate = 100.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.CU_FT, result.parsedUnit)
        assertEquals(200.0, result.billableQuantity, 0.001) // 10 * 10 * 2 * 1
        assertEquals(20000.0, result.itemAmount, 0.001)
    }

    @Test
    fun testSqMCalculation() {
        val input = RawItemInput(width = "10", height = "10", quantity = 1.0, unit = "Sq.M", rate = 100.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.SQ_M, result.parsedUnit)
        assertEquals(9.290304, result.billableQuantity, 0.001) // 10 * 10 * 0.09290304
    }

    @Test
    fun testCuMCalculation() {
        val input = RawItemInput(width = "10", height = "10", depth = "10", quantity = 1.0, unit = "Cu.M", rate = 100.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.CU_M, result.parsedUnit)
        assertEquals(28.316846592, result.billableQuantity, 0.001) // 1000 * 0.028316846592
    }

    @Test
    fun testMeterCalculation() {
        val input = RawItemInput(width = "10", quantity = 2.0, unit = "Meter", rate = 100.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.METER, result.parsedUnit)
        assertEquals(6.096, result.billableQuantity, 0.001) // 10 * 0.3048 * 2
    }

    @Test
    fun testRFtCalculation() {
        val input = RawItemInput(width = "10", quantity = 3.0, unit = "R.Ft", rate = 10.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.R_FT, result.parsedUnit)
        assertEquals(30.0, result.billableQuantity, 0.001) // 10 * 3
        assertEquals(300.0, result.itemAmount, 0.001)
    }

    @Test
    fun testLumpsumCalculation() {
        val input = RawItemInput(quantity = 1.5, unit = "Lumpsum", rate = 500.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.LUMPSUM, result.parsedUnit)
        assertEquals(1.5, result.billableQuantity, 0.001)
        assertEquals(750.0, result.itemAmount, 0.001)
    }

    @Test
    fun testNosCalculation() {
        val input = RawItemInput(quantity = 5.0, unit = "Nos", rate = 10.0)
        val result = engine.calculateItem(input)
        
        assertEquals(UnitType.NOS, result.parsedUnit)
        assertEquals(5.0, result.billableQuantity, 0.001)
        assertEquals(50.0, result.itemAmount, 0.001)
    }

    @Test
    fun testInvalidNegativeInputs() {
        val input = RawItemInput(width = "10", height = "10", quantity = -5.0, unit = "Sq.Ft", rate = -50.0)
        val result = engine.calculateItem(input)
        
        assertEquals(0.0, result.billableQuantity, 0.001)
        assertEquals(0.0, result.itemAmount, 0.001)
    }
}
