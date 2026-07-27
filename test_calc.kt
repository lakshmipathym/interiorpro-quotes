fun main() {
    val subtotal = 100.0
    val discount = 10.0 // 10% or 10 rupees?
    val gstRate = 18.0
    val taxableAmount = maxOf(0.0, subtotal - discount)
    val gstAmount = taxableAmount * (gstRate / 100.0)
    val grandTotalRaw = maxOf(0.0, subtotal - discount + gstAmount)
    println("Grand Total: $grandTotalRaw")
}
