import re

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "r") as f:
    content = f.read()

# Find the start of the chunking logic
start_str = "            val descChunks = mutableListOf<List<String>>()"
end_str = "        engine.inTableMode = false"

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx == -1 or end_idx == -1:
    print("Could not find start or end strings")
    exit(1)

new_logic = """
            val col1Nodes = mutableListOf<ColNode>()
            itemNameLines.forEach { col1Nodes.add(TextNode(it, bodyBoldPaint, 6f, 11f)) }
            if (descLines.isNotEmpty() && itemNameLines.isNotEmpty()) col1Nodes.add(SpaceNode(2f))
            descLines.forEach { col1Nodes.add(TextNode(it, descPaint, 6f, 11f)) }

            val col2Nodes = mutableListOf<ColNode>()
            specsWrapped.forEachIndexed { sIdx, (label, lines) ->
                if (sIdx > 0) col2Nodes.add(SpaceNode(2f))
                lines.forEachIndexed { idx, line ->
                    if (idx == 0) col2Nodes.add(SpecNode(label, line, specPaint, specPaint, maxLabelWidth, 10.5f))
                    else col2Nodes.add(TextNode(line, specPaint, 4f + maxLabelWidth + 8f, 10.5f))
                }
            }

            val col3Nodes = mutableListOf<ColNode>()
            sizeLinesStr.forEach { line ->
                if (line.isNotBlank()) {
                    val paint = if (line.contains("Area:") || line.contains("Volume:") || line.contains("Length:") || line.contains("Area :") || line.contains("Volume :") || line.contains("Length :")) engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = android.graphics.Paint.Align.CENTER) else engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_NORMAL, textAlign = android.graphics.Paint.Align.CENTER)
                    col3Nodes.add(TextNode(line, paint, 27.5f, 10.5f, isCentered = true))
                }
            }

            var remainingCol1 = col1Nodes.toList()
            var remainingCol2 = col2Nodes.toList()
            var remainingCol3 = col3Nodes.toList()
            var isFirstSlice = true

            // Keep formatting for first slice
            val displayQty = if (isSnapshotMode) item.billableQuantity else (if (item.rate > 0) item.amount / item.rate else com.example.engine.QuotationCalculationEngine.calculateQuantity(specs.width, specs.height, item.quantity, item.unit, specs.depth))
            val qtyRounded = Math.round(displayQty * 100.0) / 100.0
            val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", qtyRounded) else String.format(java.util.Locale.US, "%.2f", qtyRounded)
            val displayUnit = when (item.unit.trim().uppercase(java.util.Locale.US)) {
                "SQ_FT" -> "sq.ft."
                "CU_FT" -> "cu.ft."
                "R.M", "RM" -> "r.m."
                "R.FT", "RFT" -> "r.ft."
                else -> item.unit.trim().replace("_", " ")
            }
            val rateUnitDisplay = item.unit.trim().ifEmpty { "Unit" }
            val rateStr = formatIndianCurrency(item.rate) + " / " + rateUnitDisplay
            val amtStr = formatIndianCurrency(item.amount)

            while (remainingCol1.isNotEmpty() || remainingCol2.isNotEmpty() || remainingCol3.isNotEmpty()) {
                var availableSpace = engine.maxContentY - engine.currentY - 14f
                if (availableSpace < 22f) {
                    engine.startNewPage(reserveHeader = true)
                    availableSpace = engine.maxContentY - engine.currentY - 14f
                }

                fun <T : ColNode> takeFit(nodes: List<T>, maxH: Float): Pair<List<T>, List<T>> {
                    var h = 0f
                    var count = 0
                    for (node in nodes) {
                        if (h + node.height > maxH) break
                        h += node.height
                        count++
                    }
                    if (count == 0 && nodes.isNotEmpty()) count = 1
                    return Pair(nodes.take(count), nodes.drop(count))
                }

                val (slice1, rem1) = takeFit(remainingCol1, availableSpace)
                val (slice2, rem2) = takeFit(remainingCol2, availableSpace)
                val (slice3, rem3) = takeFit(remainingCol3, availableSpace)

                remainingCol1 = rem1
                remainingCol2 = rem2
                remainingCol3 = rem3

                val h1 = slice1.sumOf { it.height.toDouble() }.toFloat()
                val h2 = slice2.sumOf { it.height.toDouble() }.toFloat()
                val h3 = slice3.sumOf { it.height.toDouble() }.toFloat()

                val rowHeight = maxOf(28f, maxOf(h1, h2, h3) + 14f)
                val rowY = engine.currentY
                val drawFirstSliceContent = isFirstSlice

                engine.addCommand { canvas, _, _ ->
                    if (index % 2 == 1) {
                        canvas.drawRect(colX[0], rowY, engine.endX, rowY + rowHeight, engine.getPaint(COLOR_LIGHT_BG))
                    }
                    val gridBorderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = android.graphics.Paint.Style.STROKE)
                    canvas.drawRect(colX[0], rowY, engine.endX, rowY + rowHeight, gridBorderPaint)
                    for (i in 1 until colX.size) {
                        canvas.drawLine(colX[i], rowY, colX[i], rowY + rowHeight, gridBorderPaint)
                    }

                    val topPadding = 8f
                    val cellTextY = rowY + topPadding + 7.5f

                    if (drawFirstSliceContent) {
                        canvas.drawText((index + 1).toString(), colX[0] + 11f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = android.graphics.Paint.Align.CENTER))
                        canvas.drawText(qtyStr, colX[4] + colWidths[4] / 2f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = android.graphics.Paint.Align.CENTER))
                        canvas.drawText(displayUnit, colX[4] + colWidths[4] / 2f, cellTextY + 11f, engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_ITALIC, textAlign = android.graphics.Paint.Align.CENTER))
                        canvas.drawText(rateStr, colX[5] + colWidths[5] - 4f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = android.graphics.Paint.Align.RIGHT))
                        canvas.drawText(amtStr, colX[6] + colWidths[6] - 4f, cellTextY, engine.getPaint(COLOR_PRIMARY_BLUE, 7.5f, TYPEFACE_BOLD, textAlign = android.graphics.Paint.Align.RIGHT))
                    }

                    var y1 = rowY + topPadding
                    for (node in slice1) {
                        if (node is TextNode) canvas.drawText(node.text, colX[1] + node.xOffset, y1 + 7.5f, node.paint)
                        y1 += node.height
                    }

                    var y2 = rowY + topPadding
                    for (node in slice2) {
                        when (node) {
                            is SpecNode -> {
                                canvas.drawText(node.label, colX[2] + 4f, y2 + 7.5f, node.labelPaint)
                                canvas.drawText(":", colX[2] + 4f + node.maxLabelWidth + 2f, y2 + 7.5f, node.labelPaint)
                                canvas.drawText(node.value, colX[2] + 4f + node.maxLabelWidth + 8f, y2 + 7.5f, node.valuePaint)
                            }
                            is TextNode -> canvas.drawText(node.text, colX[2] + node.xOffset, y2 + 7.5f, node.paint)
                            is SpaceNode -> {}
                        }
                        y2 += node.height
                    }

                    var y3 = rowY + topPadding
                    for (node in slice3) {
                        if (node is TextNode) {
                            val x = if (node.isCentered) colX[3] + node.xOffset else colX[3] + node.xOffset
                            canvas.drawText(node.text, x, y3 + 7.5f, node.paint)
                        }
                        y3 += node.height
                    }
                }
                engine.currentY += rowHeight
                isFirstSlice = false
            }
        }
"""

# Replace the content
new_content = content[:start_idx] + new_logic + content[end_idx:]

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
    f.write(new_content)

print("Replacement successful.")
