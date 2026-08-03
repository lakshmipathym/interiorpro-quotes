import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

pattern = re.compile(
    r'            val col1H = \(itemNameLines\.size \+ descLines\.size\) \* 11f \+ if \(descLines\.isNotEmpty\(\)\) 2f else 0f\n.*?'
    r'            engine\.currentY \+= rowHeight\n', 
    re.DOTALL
)

replace_block = """            val descChunks = mutableListOf<List<String>>()
            if (descLines.isEmpty()) {
                descChunks.add(emptyList())
            } else {
                var start = 0
                val chunkSize = 40
                while (start < descLines.size) {
                    descChunks.add(descLines.subList(start, minOf(start + chunkSize, descLines.size)))
                    start += chunkSize
                }
            }

            descChunks.forEachIndexed { chunkIdx, chunkDescLines ->
                val cItemNameLines = if (chunkIdx == 0) itemNameLines else emptyList()
                val cSpecsWrapped = if (chunkIdx == 0) specsWrapped else emptyList()
                val cSpecsList = if (chunkIdx == 0) specsList else emptyList()
                val cSizeLinesStr = if (chunkIdx == 0) sizeLinesStr else emptyList()
                val cDescLines = chunkDescLines

                val col1H = (cItemNameLines.size + cDescLines.size) * 11f + if (cDescLines.isNotEmpty() && cItemNameLines.isNotEmpty()) 2f else 0f
                val col2H = cSpecsWrapped.sumOf { it.second.size } * 10.5f + if (cSpecsList.size > 1) (cSpecsList.size - 1) * 2f else 0f
                val col3H = cSizeLinesStr.size * 10.5f

                val rowHeight = maxOf(28f, maxOf(col1H, col2H, col3H) + 14f)
                engine.ensureSpace(rowHeight, reserveHeader = true)

                val rowY = engine.currentY
                engine.addCommand { canvas, _, _ ->
                    if (index % 2 == 1) {
                        canvas.drawRect(colX[0], rowY, engine.endX, rowY + rowHeight, engine.getPaint(COLOR_LIGHT_BG))
                    }

                    val gridBorderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)
                    canvas.drawRect(colX[0], rowY, engine.endX, rowY + rowHeight, gridBorderPaint)
                    for (i in 1 until colX.size) {
                        canvas.drawLine(colX[i], rowY, colX[i], rowY + rowHeight, gridBorderPaint)
                    }

                    val topPadding = 8f
                    val cellTextY = rowY + topPadding + 7.5f

                    // Col 0: Sl No (centered)
                    if (chunkIdx == 0) {
                        canvas.drawText((index + 1).toString(), colX[0] + 11f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.CENTER))
                    }

                    // Col 1: Item Details
                    var textY = rowY + topPadding + 7.5f
                    cItemNameLines.forEach { line ->
                        canvas.drawText(line, colX[1] + 6f, textY, bodyBoldPaint)
                        textY += 11f
                    }
                    if (cDescLines.isNotEmpty() && cItemNameLines.isNotEmpty()) textY += 2f
                    cDescLines.forEach { line ->
                        canvas.drawText(line, colX[1] + 6f, textY, descPaint)
                        textY += 11f
                    }

                    // Col 2: Specifications
                    var specY = rowY + topPadding + 7.5f
                    cSpecsWrapped.forEachIndexed { sIdx, (label, lines) ->
                        if (sIdx > 0) {
                            specY += 2f
                        }
                        lines.forEachIndexed { idx, line ->
                            if (idx == 0) {
                                canvas.drawText(label, colX[2] + 4f, specY, specPaint)
                                canvas.drawText(":", colX[2] + 4f + maxLabelWidth + 2f, specY, specPaint)
                                canvas.drawText(line, colX[2] + 4f + maxLabelWidth + 8f, specY, specPaint)
                            } else {
                                canvas.drawText(line, colX[2] + 4f + maxLabelWidth + 8f, specY, specPaint)
                            }
                            specY += 10.5f
                        }
                    }

                    // Col 3: Size (perfectly aligned and centered)
                    val sizeLabelPaint = engine.getPaint(COLOR_PRIMARY_BLUE, 6.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER)
                    val sizeValuePaint = engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_NORMAL, textAlign = Paint.Align.CENTER)

                    var sizeY = rowY + topPadding + 7.5f
                    val col3CenterX = colX[3] + 27.5f
                    cSizeLinesStr.forEach { line ->
                        if (line.isNotBlank()) {
                            val paint = if (line.contains("Area:") || line.contains("Volume:") || line.contains("Length:") || line.contains("Area :") || line.contains("Volume :") || line.contains("Length :")) engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint
                            canvas.drawText(line, col3CenterX, sizeY, paint)
                        }
                        sizeY += 10.5f
                    }

                    if (chunkIdx == 0) {
                        // Col 4: Quantity
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
                        
                        canvas.drawText("$qtyStr $displayUnit", colX[4] + colWidths[4] - 4f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.RIGHT))

                        // Col 5: Rate
                        val rateUnitDisplay = item.unit.trim().ifEmpty { "Unit" }
                        val rateStr = formatIndianCurrency(item.rate) + " / " + rateUnitDisplay
                        canvas.drawText(rateStr, colX[5] + colWidths[5] - 4f, cellTextY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.RIGHT))

                        // Col 6: Amount
                        canvas.drawText(formatIndianCurrency(item.amount), colX[6] + colWidths[6] - 4f, cellTextY, engine.getPaint(COLOR_PRIMARY_BLUE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT))
                    }
                }
                engine.currentY += rowHeight
            }
"""

if pattern.search(content):
    content = pattern.sub(replace_block, content, count=1)
    with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully.")
else:
    print("Could not find the exact block via regex.")
