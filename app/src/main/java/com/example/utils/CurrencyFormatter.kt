package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    fun formatIndianCurrency(amount: Double): String {
        return try {
            val formatter = DecimalFormat("##,##,##,##0.00")
            formatter.decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
            var res = formatter.format(amount)
            if (res.endsWith(".00")) {
                res = res.substring(0, res.length - 3)
            }
            res
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", amount)
        }
    }
    
    fun formatIndianCurrencyStrict(amount: Double): String {
        return try {
            val formatter = DecimalFormat("##,##,##,##0.00")
            formatter.decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
            formatter.format(amount)
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", amount)
        }
    }
    
    fun convertNumberToWords(amount: Double): String {
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
