import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# 1. Update the billableLabel logic and rendering
old_size_logic = """            val billableLabel = when {
                uLower.contains("sq") -> "Billable Area :"
                uLower.contains("cu") -> "Billable Volume :"
                uLower.contains("ft") || uLower.contains("meter") -> "Running Length :"
                else -> ""
            }
            if (billableLabel.isNotEmpty()) {
                if (sizeLinesStr.isNotEmpty()) sizeLinesStr.add("") // empty line for spacing
                sizeLinesStr.add(billableLabel)
                val qtyRounded = Math.round(billableQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%.0f %s", qtyRounded, item.unit.trim()) else String.format(Locale.US, "%.2f %s", qtyRounded, item.unit.trim())
                val qtyWrappedLines = engine.wrapText(qtyStr, (colWidths[3] - 4f).toInt(), engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_BOLD))
                sizeLinesStr.addAll(qtyWrappedLines)
            }"""

new_size_logic = """            val billableLabel = when {
                uLower.contains("sq") -> "Billable Area:"
                uLower.contains("cu") -> "Billable Volume:"
                uLower.contains("ft") || uLower.contains("meter") || uLower == "r.m" || uLower == "rm" -> "Running Length:"
                else -> ""
            }
            if (billableLabel.isNotEmpty()) {
                if (sizeLinesStr.isNotEmpty()) sizeLinesStr.add("") // empty line for spacing
                
                val qtyRounded = Math.round(billableQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%.0f %s", qtyRounded, item.unit.trim()) else String.format(Locale.US, "%.2f %s", qtyRounded, item.unit.trim())
                
                // Wrap the label and the value
                val labelLines = engine.wrapText(billableLabel, (colWidths[3] - 4f).toInt(), engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD))
                sizeLinesStr.addAll(labelLines)
                val qtyWrappedLines = engine.wrapText(qtyStr, (colWidths[3] - 4f).toInt(), engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_BOLD))
                sizeLinesStr.addAll(qtyWrappedLines)
            }"""

content = content.replace(old_size_logic, new_size_logic)

old_size_draw = """                sizeLinesStr.forEach { line ->
                    if (line == "Size") {
                        canvas.drawText(line, col3CenterX, sizeY, sizeLabelPaint)
                    } else if (line.startsWith("Area :") || line.startsWith("Running Length :") || line.startsWith("Volume :")) {
                        canvas.drawText(line, col3CenterX, sizeY, sizeLabelPaint)
                    } else if (line.isNotBlank()) {
                        canvas.drawText(line, col3CenterX, sizeY, sizeValuePaint)
                    }
                    sizeY += 10.5f
                }"""

new_size_draw = """                sizeLinesStr.forEach { line ->
                    if (line == "Size") {
                        canvas.drawText(line, col3CenterX, sizeY, sizeLabelPaint)
                    } else if (line.startsWith("Billable") || line.startsWith("Running Length")) {
                        val paint = engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER)
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    } else if (line.isNotBlank()) {
                        val paint = if (line.contains(item.unit.trim())) engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    }
                    sizeY += 10.5f
                }"""

content = content.replace(old_size_draw, new_size_draw)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
