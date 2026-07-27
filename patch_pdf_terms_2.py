import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Replace the specific block that sets tY for the terms items
old_tY = """        wrappedTerms.forEachIndexed { index, wt ->
            val item = wt.item
            engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)
            val tY = engine.currentY
            engine.addCommand { canvas, _, _ ->"""

new_tY = """        wrappedTerms.forEachIndexed { index, wt ->
            val item = wt.item
            if (index > 0) {
                engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)
            }
            val tY = engine.currentY + 8f // text baseline needs offset down from top boundary
            engine.addCommand { canvas, _, _ ->"""

content = content.replace(old_tY, new_tY)

old_ensure = """        val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
        engine.ensureSpace(titleHeight + firstTermH, reserveHeader = true)

        val blockTop = engine.currentY
        engine.addCommand { canvas, _, _ ->
            val titlePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 8.5f, TYPEFACE_BOLD)
            canvas.drawText("TERMS & CONDITIONS", leftX, blockTop + 8f, titlePaint)
        }
        engine.currentY += 12f"""

new_ensure = """        val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
        engine.ensureSpace(titleHeight + firstTermH + 8f, reserveHeader = true)

        val blockTop = engine.currentY
        engine.addCommand { canvas, _, _ ->
            val titlePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 8.5f, TYPEFACE_BOLD)
            canvas.drawText("TERMS & CONDITIONS", leftX, blockTop + 8f, titlePaint)
        }
        engine.currentY += 16f"""
content = content.replace(old_ensure, new_ensure)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
