package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun testFormatIndianCurrency() {
        assertEquals("1,32,491", CurrencyFormatter.formatIndianCurrency(132491.0))
        assertEquals("1,32,491.50", CurrencyFormatter.formatIndianCurrency(132491.50))
    }

    @Test
    fun testConvertNumberToWords() {
        assertEquals("One Lakh Thirty Two Thousand Four Hundred and Ninety One Only", CurrencyFormatter.convertNumberToWords(132491.0))
        assertEquals("One Lakh Thirty Two Thousand Four Hundred and Ninety One and Fifty Paise Only", CurrencyFormatter.convertNumberToWords(132491.50))
    }
}
