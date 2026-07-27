package com.example.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountInWordsConverterImplTest {

    private val converter = AmountInWordsConverterImpl()

    @Test
    fun testZero() {
        assertEquals("Zero Only", converter.convertToWords(0.0))
    }

    @Test
    fun testBasicAmounts() {
        assertEquals("One Hundred Only", converter.convertToWords(100.0))
        assertEquals("One Thousand One Hundred and Eleven Only", converter.convertToWords(1111.0))
        assertEquals("One Lakh Only", converter.convertToWords(100000.0))
        assertEquals("One Crore Only", converter.convertToWords(10000000.0))
    }

    @Test
    fun testPaise() {
        assertEquals("One Hundred and Fifty Paise Only", converter.convertToWords(100.50))
        assertEquals("Zero and Twenty Five Paise Only", converter.convertToWords(0.25))
    }
}
