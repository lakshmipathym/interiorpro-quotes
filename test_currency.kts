import java.text.NumberFormat
import java.util.Locale
import java.text.DecimalFormat

val format = NumberFormat.getNumberInstance(Locale("en", "IN")) as DecimalFormat
format.applyPattern("##,##,##,##0.00")
println(format.format(132491.50))
println(format.format(132491.0))

val format2 = NumberFormat.getNumberInstance(Locale("en", "IN"))
format2.minimumFractionDigits = 2
format2.maximumFractionDigits = 2
println(format2.format(132491.50))

val format3 = NumberFormat.getNumberInstance(Locale("en", "IN"))
format3.minimumFractionDigits = 0
format3.maximumFractionDigits = 2
println(format3.format(132491.0))
println(format3.format(132491.50))

