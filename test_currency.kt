import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun main() {
    val formatter = DecimalFormat("##,##,##,##0.00")
    formatter.decimalFormatSymbols = DecimalFormatSymbols(Locale.US)
    var res = formatter.format(132491.0)
    if (res.endsWith(".00")) {
        res = res.substring(0, res.length - 3)
    }
    println(res)
}
