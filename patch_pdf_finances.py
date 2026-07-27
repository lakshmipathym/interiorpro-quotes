import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Replace the beginning of drawSummaryAndPayment to recalculate the totals
old_totals = """        val rowH = 16f
        val totalsRows = mutableListOf<Pair<String, String>>()
        totalsRows.add(Pair("Sub Total", "₹ " + formatIndianCurrency(quotation.subtotal)))
        if (quotation.discount > 0.0) {
            totalsRows.add(Pair("Discount", "₹ " + formatIndianCurrency(quotation.discount)))
        }
        if (showGst && (quotation.gstAmount > 0.0 || quotation.gstRate > 0.0)) {
            totalsRows.add(Pair("GST (${quotation.gstRate}%)", "₹ " + formatIndianCurrency(quotation.gstAmount)))
        }
        if (quotation.transport > 0.0) {
            totalsRows.add(Pair("Transport", "₹ " + formatIndianCurrency(quotation.transport)))
        }
        if (quotation.installation > 0.0) {
            totalsRows.add(Pair("Installation", "₹ " + formatIndianCurrency(quotation.installation)))
        }
        if (quotation.extraCharges > 0.0) {
            totalsRows.add(Pair("Extra Charges", "₹ " + formatIndianCurrency(quotation.extraCharges)))
        }
        if (Math.abs(quotation.roundOff) > 0.001) {
            totalsRows.add(Pair("Round Off", "₹ " + formatIndianCurrency(quotation.roundOff)))
        }"""

new_totals = """        val rowH = 16f
        val totalsRows = mutableListOf<Pair<String, String>>()
        
        // Recalculate totals from items
        val calculatedSubtotal = items.sumOf { it.amount }
        val discount = quotation.discount
        val taxableAmount = calculatedSubtotal - discount
        
        val gstAmount = if (showGst && quotation.gstRate > 0.0) {
            taxableAmount * (quotation.gstRate / 100.0)
        } else if (showGst && quotation.gstAmount > 0.0) {
            quotation.gstAmount
        } else {
            0.0
        }
        
        val transport = quotation.transport
        val installation = quotation.installation
        val extraCharges = quotation.extraCharges
        
        val rawGrandTotal = calculatedSubtotal - discount + gstAmount + transport + installation + extraCharges
        val calculatedGrandTotal = Math.round(rawGrandTotal).toDouble()
        val roundOff = calculatedGrandTotal - rawGrandTotal
        
        val normalizedFinalGrandTotal = Math.round(calculatedGrandTotal * 100.0) / 100.0
        val balanceDue = normalizedFinalGrandTotal - quotation.advance
        
        totalsRows.add(Pair("Sub Total", "₹ " + formatIndianCurrency(calculatedSubtotal)))
        if (discount > 0.0) {
            totalsRows.add(Pair("Discount", "₹ " + formatIndianCurrency(discount)))
        }
        if (showGst && (gstAmount > 0.0 || quotation.gstRate > 0.0)) {
            totalsRows.add(Pair("GST (${quotation.gstRate}%)", "₹ " + formatIndianCurrency(gstAmount)))
        }
        if (transport > 0.0) {
            totalsRows.add(Pair("Transport", "₹ " + formatIndianCurrency(transport)))
        }
        if (installation > 0.0) {
            totalsRows.add(Pair("Installation", "₹ " + formatIndianCurrency(installation)))
        }
        if (extraCharges > 0.0) {
            totalsRows.add(Pair("Extra Charges", "₹ " + formatIndianCurrency(extraCharges)))
        }
        if (Math.abs(roundOff) > 0.001) {
            totalsRows.add(Pair("Round Off", "₹ " + formatIndianCurrency(roundOff)))
        }"""
        
content = content.replace(old_totals, new_totals)

old_normalized = "val normalizedFinalGrandTotal = Math.round(quotation.grandTotal * 100.0) / 100.0"
content = content.replace(old_normalized, "")

old_gt_draw = 'canvas.drawText("₹ " + formatIndianCurrency(quotation.balance), engine.endX - 10f, balanceY, textWhiteBold8_5Right)'
new_gt_draw = 'canvas.drawText("₹ " + formatIndianCurrency(balanceDue), engine.endX - 10f, balanceY, textWhiteBold8_5Right)'
content = content.replace(old_gt_draw, new_gt_draw)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
