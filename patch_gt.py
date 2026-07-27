import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Make sure grand total logic aligns correctly
old_gt_code = """        val normalizedFinalGrandTotal = Math.round(quotation.grandTotal * 100.0) / 100.0"""

new_gt_code = """        val rawGrandTotal = quotation.subtotal - quotation.discount + quotation.gstAmount + quotation.transport + quotation.installation + quotation.extraCharges
        val calculatedGrandTotal = Math.round(rawGrandTotal).toDouble()
        val normalizedFinalGrandTotal = Math.round(calculatedGrandTotal * 100.0) / 100.0
        val balanceDue = normalizedFinalGrandTotal - quotation.advance"""

content = content.replace(old_gt_code, new_gt_code)

old_gt_draw = 'canvas.drawText("₹ " + formatIndianCurrency(quotation.balance), engine.endX - 10f, balanceY, textWhiteBold8_5Right)'
new_gt_draw = 'canvas.drawText("₹ " + formatIndianCurrency(balanceDue), engine.endX - 10f, balanceY, textWhiteBold8_5Right)'
content = content.replace(old_gt_draw, new_gt_draw)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
