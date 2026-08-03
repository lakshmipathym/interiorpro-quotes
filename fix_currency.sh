cat << 'INNEREOF' > app/src/main/java/com/example/utils/CurrencyFormatter.kt
package com.example.utils

import java.util.Locale

object CurrencyFormatter {
    fun formatIndianCurrency(amount: Double): String {
        return try {
            val amountLong = amount.toLong()
            val fraction = amount - amountLong
            var s = amountLong.toString()
            var res = ""
            var count = 0
            for (i in s.length - 1 downTo 0) {
                res = s[i] + res
                count++
                if (count == 3 && i > 0) {
                    res = "," + res
                } else if (count > 3 && (count - 3) % 2 == 0 && i > 0) {
                    res = "," + res
                }
            }
            if (fraction > 0.001) {
                res += String.format(Locale.US, "%.2f", fraction).substring(1)
            }
            res
        } catch (e: Exception) {
            amount.toString()
        }
    }
    
    fun formatIndianCurrencyStrict(amount: Double): String {
        val base = formatIndianCurrency(amount)
        return if (!base.contains(".")) "$base.00" else {
            val parts = base.split(".")
            if (parts[1].length == 1) "${base}0" else base
        }
    }
    
    fun convertNumberToWords(amount: Double): String {
        val num = amount.toLong()
        val paise = Math.round((amount - num) * 100.0).toLong()
        
        var result = if (num == 0L) "Zero" else convertToWords(num)
        
        if (paise > 0L) {
            result += " and " + convertToWords(paise) + " Paise"
        }
        
        return "$result Only"
    }

    private val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
    private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    private fun convertToWords(n: Long): String {
        if (n < 20) return units[n.toInt()]
        if (n < 100) return tens[(n / 10).toInt()] + if (n % 10 != 0L) " " + units[(n % 10).toInt()] else ""
        if (n < 1000) return units[(n / 100).toInt()] + " Hundred" + if (n % 100 != 0L) " and " + convertToWords(n % 100) else ""
        if (n < 100000) return convertToWords(n / 1000) + " Thousand" + if (n % 1000 != 0L) " " + convertToWords(n % 1000) else ""
        if (n < 10000000) return convertToWords(n / 100000) + " Lakh" + if (n % 100000 != 0L) " " + convertToWords(n % 100000) else ""
        return convertToWords(n / 10000000) + " Crore" + if (n % 10000000 != 0L) " " + convertToWords(n % 10000000) else ""
    }
}
INNEREOF
