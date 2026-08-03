package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    fun normalizeCurrency(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        return Math.round(amount * 100.0) / 100.0
    }

    fun formatIndianCurrency(amount: Double): String {
        return formatAmountInternal(amount, false)
    }
    
    fun formatIndianCurrencyStrict(amount: Double): String {
        return formatAmountInternal(amount, true)
    }
    
    private fun formatAmountInternal(rawAmount: Double, strict: Boolean): String {
        val amount = normalizeCurrency(rawAmount)
        return try {
            val isNegative = amount < 0
            val absAmount = Math.abs(amount)
            val formatter = DecimalFormat("0.00")
            formatter.decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
            val res = formatter.format(absAmount)
            
            val split = res.split(".")
            var intPart = split[0]
            val decimalPart = split[1]
            
            var formattedInt = ""
            if (intPart.length > 3) {
                formattedInt = "," + intPart.substring(intPart.length - 3)
                intPart = intPart.substring(0, intPart.length - 3)
                while (intPart.length > 2) {
                    formattedInt = "," + intPart.substring(intPart.length - 2) + formattedInt
                    intPart = intPart.substring(0, intPart.length - 2)
                }
                formattedInt = intPart + formattedInt
            } else {
                formattedInt = intPart
            }
            
            val finalSign = if (isNegative) "-" else ""
            if (!strict && decimalPart == "00") {
                finalSign + formattedInt
            } else {
                finalSign + formattedInt + "." + decimalPart
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", amount)
        }
    }
    
    fun convertNumberToWords(rawAmount: Double): String {
        val amount = normalizeCurrency(rawAmount)
        val num = amount.toLong()
        val paise = Math.round((amount - num) * 100.0).toLong()
        
        var result = if (num == 0L) "Zero" else convertToWords(num)
        
        if (paise > 0L) {
            result += " and " + convertToWords(paise) + " Paise"
        }
        return result + " Only"
    }
    
    private val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
    private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
    
    private fun convertToWords(n: Long): String {
        if (n < 0) return "Minus " + convertToWords(-n)
        if (n < 20) return units[n.toInt()]
        if (n < 100) return tens[(n / 10).toInt()] + (if (n % 10 != 0L) " " + units[(n % 10).toInt()] else "")
        if (n < 1000) return units[(n / 100).toInt()] + " Hundred" + (if (n % 100 != 0L) " and " + convertToWords(n % 100) else "")
        if (n < 100000) return convertToWords(n / 1000) + " Thousand" + (if (n % 1000 != 0L) " " + convertToWords(n % 1000) else "")
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh" + (if (n % 100000 != 0L) " " + convertToWords(n % 100000) else "")
        return convertToWords(n / 10000000) + " Crore" + (if (n % 10000000 != 0L) " " + convertToWords(n % 10000000) else "")
    }
}
