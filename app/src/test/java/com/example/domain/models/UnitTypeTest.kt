package com.example.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitTypeTest {

    @Test
    fun testSqFtParsing() {
        assertEquals(UnitType.SQ_FT, UnitType.fromString("Sq.Ft"))
        assertEquals(UnitType.SQ_FT, UnitType.fromString("sqft"))
        assertEquals(UnitType.SQ_FT, UnitType.fromString("Square Feet"))
        assertEquals(UnitType.SQ_FT, UnitType.fromString("SFT"))
    }

    @Test
    fun testCuFtParsing() {
        assertEquals(UnitType.CU_FT, UnitType.fromString("Cu.Ft"))
        assertEquals(UnitType.CU_FT, UnitType.fromString("cuft"))
        assertEquals(UnitType.CU_FT, UnitType.fromString("Cubic Feet"))
        assertEquals(UnitType.CU_FT, UnitType.fromString("CFT"))
    }

    @Test
    fun testSqMParsing() {
        assertEquals(UnitType.SQ_M, UnitType.fromString("Sq.M"))
        assertEquals(UnitType.SQ_M, UnitType.fromString("sqm"))
        assertEquals(UnitType.SQ_M, UnitType.fromString("Square Meter"))
        assertEquals(UnitType.SQ_M, UnitType.fromString("Square Mtr"))
    }

    @Test
    fun testCuMParsing() {
        assertEquals(UnitType.CU_M, UnitType.fromString("Cu.M"))
        assertEquals(UnitType.CU_M, UnitType.fromString("cum"))
        assertEquals(UnitType.CU_M, UnitType.fromString("Cubic Meter"))
        assertEquals(UnitType.CU_M, UnitType.fromString("Cubic Mtr"))
    }

    @Test
    fun testMeterParsing() {
        assertEquals(UnitType.METER, UnitType.fromString("Meter"))
        assertEquals(UnitType.METER, UnitType.fromString("mtr"))
        assertEquals(UnitType.METER, UnitType.fromString("R.M"))
        assertEquals(UnitType.METER, UnitType.fromString("RM"))
    }

    @Test
    fun testRFtParsing() {
        assertEquals(UnitType.R_FT, UnitType.fromString("Running Feet"))
        assertEquals(UnitType.R_FT, UnitType.fromString("R.Ft"))
        assertEquals(UnitType.R_FT, UnitType.fromString("RFT"))
        assertEquals(UnitType.R_FT, UnitType.fromString("R.F"))
    }

    @Test
    fun testLumpsumParsing() {
        assertEquals(UnitType.LUMPSUM, UnitType.fromString("Lumpsum"))
        assertEquals(UnitType.LUMPSUM, UnitType.fromString("Lump Sum"))
        assertEquals(UnitType.LUMPSUM, UnitType.fromString("L.S"))
        assertEquals(UnitType.LUMPSUM, UnitType.fromString("LS"))
    }

    @Test
    fun testDefaultParsing() {
        assertEquals(UnitType.NOS, UnitType.fromString("Nos"))
        assertEquals(UnitType.NOS, UnitType.fromString("Pcs"))
        assertEquals(UnitType.NOS, UnitType.fromString("RandomString"))
        assertEquals(UnitType.NOS, UnitType.fromString(""))
    }
}
