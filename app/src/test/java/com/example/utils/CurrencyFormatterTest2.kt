package com.example.utils
import org.junit.Assert.assertEquals
import org.junit.Test
class CurrencyFormatterTest2 {
    @Test
    fun test() {
        val amount = 132491.50
        val s = String.format(java.util.Locale.US, "%.2f", amount)
        val parts = s.split(".")
        var integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""
        
        val res = StringBuilder()
        var count = 0
        for (i in integerPart.length - 1 downTo 0) {
            res.append(integerPart[i])
            count++
            if (count == 3 && i > 0) {
                res.append(",")
            } else if (count > 3 && (count - 3) % 2 == 0 && i > 0) {
                res.append(",")
            }
        }
        val finalStr = res.reverse().toString() + decimalPart
        println("IN_FORMAT: " + finalStr)
    }
}
