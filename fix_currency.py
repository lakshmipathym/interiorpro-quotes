import sys

file_path = "/app/applet/app/src/main/java/com/example/utils/CurrencyFormatter.kt"
with open(file_path, "r") as f:
    content = f.read()

target = """    fun formatIndianCurrency(amount: Double): String {
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
    }"""

replacement = """    fun formatIndianCurrency(amount: Double): String {
        return try {
            val s = String.format(Locale.US, "%.2f", amount)
            val parts = s.split(".")
            val integerPart = parts[0]
            val decimalPart = if (parts.size > 1) "." + parts[1] else ""
            val res = java.lang.StringBuilder()
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
            var finalStr = res.reverse().toString() + decimalPart
            if (finalStr.endsWith(".00")) {
                finalStr = finalStr.substring(0, finalStr.length - 3)
            }
            finalStr
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", amount)
        }
    }
    
    fun formatIndianCurrencyStrict(amount: Double): String {
        return try {
            val s = String.format(Locale.US, "%.2f", amount)
            val parts = s.split(".")
            val integerPart = parts[0]
            val decimalPart = if (parts.size > 1) "." + parts[1] else ""
            val res = java.lang.StringBuilder()
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
            res.reverse().toString() + decimalPart
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f", amount)
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(file_path, "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
