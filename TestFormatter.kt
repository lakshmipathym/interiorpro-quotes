import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun main() {
    val formatter = DecimalFormat("##,##,##,##0.00")
    formatter.decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
    println(formatter.format(132491.0))
}
