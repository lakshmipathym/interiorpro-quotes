package com.example.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class DimensionParserImplTest {

    private val parser = DimensionParserImpl()

    @Test
    fun parseSimpleNumbers() {
        assertEquals(10.5, parser.parseToFeet("10.5"), 0.001)
        assertEquals(0.0, parser.parseToFeet("0"), 0.001)
        assertEquals(0.0, parser.parseToFeet(""), 0.001)
        assertEquals(100.0, parser.parseToFeet("100"), 0.001)
    }

    @Test
    fun parseFtSuffix() {
        assertEquals(10.0, parser.parseToFeet("10 ft"), 0.001)
        assertEquals(10.0, parser.parseToFeet("10ft"), 0.001)
        assertEquals(10.5, parser.parseToFeet("10.5 feet"), 0.001)
    }

    @Test
    fun parseFeetAndInches() {
        assertEquals(10.5, parser.parseToFeet("10' 6\""), 0.001)
        assertEquals(10.0, parser.parseToFeet("10'"), 0.001)
        assertEquals(10.25, parser.parseToFeet("10' 3\""), 0.001)
    }

    @Test
    fun parseInvalidInput() {
        assertEquals(0.0, parser.parseToFeet("abc"), 0.001)
        assertEquals(10.0, parser.parseToFeet("10' abc\""), 0.001)
    }
}
