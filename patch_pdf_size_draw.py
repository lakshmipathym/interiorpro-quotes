import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

old_draw = """                        val paint = if (line.contains(item.unit.trim())) engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint"""
new_draw = """                        val unitStr = item.unit.trim()
                        val paint = if (unitStr.isNotEmpty() && line.contains(unitStr)) engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint"""

content = content.replace(old_draw, new_draw)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
