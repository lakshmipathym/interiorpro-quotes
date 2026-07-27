import re

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "r") as f:
    content = f.read()

# Item details (Col 1)
content = content.replace(
    'val col1H = (itemNameLines.size + descLines.size) * 11f',
    'val col1H = (itemNameLines.size + descLines.size) * 11f + if (descLines.isNotEmpty()) 2f else 0f'
)

old_item_render = """                // Col 1: Item Details
                var textY = rowY + topPadding + 7.5f
                itemNameLines.forEach { line ->
                    canvas.drawText(line, colX[1] + 4f, textY, bodyBoldPaint)
                    textY += 11f
                }
                descLines.forEach { line ->
                    canvas.drawText(line, colX[1] + 4f, textY, descPaint)
                    textY += 11f
                }"""
new_item_render = """                // Col 1: Item Details
                var textY = rowY + topPadding + 7.5f
                itemNameLines.forEach { line ->
                    canvas.drawText(line, colX[1] + 6f, textY, bodyBoldPaint)
                    textY += 11f
                }
                if (descLines.isNotEmpty()) textY += 2f
                descLines.forEach { line ->
                    canvas.drawText(line, colX[1] + 6f, textY, descPaint)
                    textY += 11f
                }"""
content = content.replace(old_item_render, new_item_render)

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
    f.write(content)

