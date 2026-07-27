import re

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "r") as f:
    content = f.read()

# Update Table Header for Qty
old_header_align = """                for (i in headers.indices) {
                    if (i == 5 || i == 6) {
                        val cx = colX[i] + colWidths[i] - 4f
                        val rightPaint = getPaint(COLOR_WHITE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)
                        canvas.drawText(headers[i], cx, centerY, rightPaint)
                    } else {"""
new_header_align = """                for (i in headers.indices) {
                    if (i == 4 || i == 5 || i == 6) {
                        val cx = colX[i] + colWidths[i] - 4f
                        val rightPaint = getPaint(COLOR_WHITE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)
                        canvas.drawText(headers[i], cx, centerY, rightPaint)
                    } else {"""
content = content.replace(old_header_align, new_header_align)

# Update Items Row for Qty
old_qty_draw = """                // Col 4: Qty
                val userQty = getCalculatedArea(item, specs)
                val qtyRounded = Math.round(userQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%,.0f", qtyRounded) else String.format(Locale.US, "%,.2f", qtyRounded)
                canvas.drawText(qtyStr, colX[4] + 17.5f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.CENTER))"""
new_qty_draw = """                // Col 4: Qty
                val userQty = getCalculatedArea(item, specs)
                val qtyRounded = Math.round(userQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%,.0f", qtyRounded) else String.format(Locale.US, "%,.2f", qtyRounded)
                canvas.drawText(qtyStr, colX[4] + colWidths[4] - 4f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.RIGHT))"""
content = content.replace(old_qty_draw, new_qty_draw)

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
    f.write(content)

