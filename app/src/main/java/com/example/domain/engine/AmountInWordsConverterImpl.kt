package com.example.domain.engine

import com.example.domain.contracts.AmountInWordsConverter
import kotlin.math.roundToLong

class AmountInWordsConverterImpl : AmountInWordsConverter {

    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    override fun convertToWords(amount: Double): String {
        val num = amount.toLong()
        val paise = ((amount - num) * 100.0).roundToLong()

        var result = if (num == 0L) "Zero" else convertNumber(num)

        if (paise > 0L) {
            result += " and " + convertNumber(paise) + " Paise"
        }
        return result + " Only"
    }

    private fun convertNumber(n: Long): String {
        if (n < 0) return "Minus " + convertNumber(-n)
        if (n < 20) return units[n.toInt()]
        if (n < 100) return tens[(n / 10).toInt()] + (if (n % 10 != 0L) " " + units[(n % 10).toInt()] else "")
        if (n < 1000) return units[(n / 100).toInt()] + " Hundred" + (if (n % 100 != 0L) " and " + convertNumber(n % 100) else "")
        if (n < 100000) return convertNumber(n / 1000) + " Thousand" + (if (n % 1000 != 0L) " " + convertNumber(n % 1000) else "")
        if (n < 10000000) return convertNumber(n / 100000) + " Lakh" + (if (n % 100000 != 0L) " " + convertNumber(n % 100000) else "")
        return convertNumber(n / 10000000) + " Crore" + (if (n % 10000000 != 0L) " " + convertNumber(n % 10000000) else "")
    }
}
