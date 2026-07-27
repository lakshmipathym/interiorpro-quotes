import re

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "r") as f:
    content = f.read()

# Update left padding and wrap text widths in Items Table
content = content.replace(
    'val lines = engine.wrapText(spec, 147, specPaint)',
    'val lines = engine.wrapText(spec, 140, specPaint)'
)

content = content.replace(
    'val col2H = specsWrapped.size * 9.5f',
    'val col2H = specsWrapped.size * 10.5f + (specsList.size - 1) * 2f'
)

# Render Specs (Col 2)
old_spec_render = """                // Col 2: Specifications
                var specY = rowY + topPadding + 7.5f
                specsWrapped.forEach { (isFirst, line) ->
                    if (isFirst) {
                        canvas.drawText("• $line", colX[2] + 4f, specY, specPaint)
                    } else {
                        canvas.drawText(line, colX[2] + 12f, specY, specPaint)
                    }
                    specY += 9.5f
                }"""
new_spec_render = """                // Col 2: Specifications
                var specY = rowY + topPadding + 7.5f
                specsWrapped.forEachIndexed { sIdx, (isFirst, line) ->
                    if (isFirst && sIdx > 0) {
                        specY += 2f
                    }
                    if (isFirst) {
                        canvas.drawText("•  $line", colX[2] + 6f, specY, specPaint)
                    } else {
                        canvas.drawText(line, colX[2] + 16f, specY, specPaint)
                    }
                    specY += 10.5f
                }"""
content = content.replace(old_spec_render, new_spec_render)

# Size lines (Col 3) - increase sizeY step and set col3H correctly
content = content.replace(
    'val col3H = sizeLines.size * 9.5f',
    'val col3H = sizeLines.size * 10.5f'
)

old_size_render = """                var sizeY = rowY + topPadding + 7.5f
                sizeLines.forEach { line ->
                    canvas.drawText(line.first, startX, sizeY, sizeLabelPaint)
                    canvas.drawText(colonAndSpace, startX + maxNameWidth, sizeY, sizeLabelPaint)
                    canvas.drawText(line.second, startX + maxNameWidth + colonWidth, sizeY, sizeValuePaint)
                    sizeY += 9.5f
                }"""
new_size_render = """                var sizeY = rowY + topPadding + 7.5f
                sizeLines.forEach { line ->
                    canvas.drawText(line.first, startX, sizeY, sizeLabelPaint)
                    canvas.drawText(colonAndSpace, startX + maxNameWidth, sizeY, sizeLabelPaint)
                    canvas.drawText(line.second, startX + maxNameWidth + colonWidth, sizeY, sizeValuePaint)
                    sizeY += 10.5f
                }"""
content = content.replace(old_size_render, new_size_render)


with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
    f.write(content)

