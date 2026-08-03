sed -i 's/return Math.round(gst \* 100.0) \/ 100.0/return com.example.utils.CurrencyFormatter.normalizeCurrency(gst)/g' app/src/main/java/com/example/engine/TaxEngine.kt
sed -i 's/val taxableAmount = maxOf(0.0, subtotal - discount)/val taxableAmount = com.example.utils.CurrencyFormatter.normalizeCurrency(maxOf(0.0, subtotal - discount))/g' app/src/main/java/com/example/engine/TaxEngine.kt
