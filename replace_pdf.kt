val subtotal = com.example.engine.QuotationCalculationEngine.calculateSubtotal(validatedItems)
val gstAmount = if (showGst) com.example.engine.TaxEngine.calculateGstAmount(subtotal, quotation.discount, quotation.gstRate) else 0.0
val grandTotalRounded = com.example.engine.QuotationCalculationEngine.calculateGrandTotal(subtotal, quotation.discount, gstAmount)
val roundOff = Math.round(grandTotalRounded).toDouble() - grandTotalRounded
