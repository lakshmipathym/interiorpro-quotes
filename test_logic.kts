fun format(amount: Double, strict: Boolean): String {
    val isNegative = amount < 0
    val absAmount = Math.abs(amount)
    val formatter = java.text.DecimalFormat("0.00")
    formatter.decimalFormatSymbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
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
    return if (!strict && decimalPart == "00") {
        finalSign + formattedInt
    } else {
        finalSign + formattedInt + "." + decimalPart
    }
}

println(format(132491.50, false))
println(format(132491.0, false))
println(format(1000.0, false))
println(format(10000.0, false))
println(format(100000.0, false))
println(format(1000000.0, false))

println(format(132491.50, true))
println(format(132491.0, true))
println(format(1000.0, true))
println(format(10000.0, true))
println(format(100000.0, true))
println(format(1000000.0, true))
