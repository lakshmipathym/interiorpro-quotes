import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

pattern = re.compile(
    r'        wrappedTerms\.forEachIndexed \{ index, wt ->\n'
    r'            val item = wt\.item\n'
    r'            engine\.ensureSpace\(wt\.itemH \+ termSpacing, reserveHeader = true\)\n.*?'
    r'            \}\n'
    r'            engine\.currentY \+= wt\.itemH \+ termSpacing\n'
    r'        \}\n',
    re.DOTALL
)

replace_block = """        wrappedTerms.forEachIndexed { index, wt ->
            val item = wt.item
            
            // Check if the term is too tall for a page and needs chunking (mostly bullets or valLines)
            val maxLinesPerChunk = 50
            val maxRows = maxOf(wt.labelLines.size, wt.valLines.size, wt.bulletsWrapped.sumOf { it.size })
            
            val numChunks = (maxRows + maxLinesPerChunk - 1) / maxLinesPerChunk
            if (numChunks <= 1) {
                engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)
                val tY = engine.currentY + 7f
                engine.addCommand { canvas, _, _ ->
                    val textPaint = engine.getPaint(COLOR_DARK_SLATE, 7f, TYPEFACE_NORMAL)
                    val hasValueOrBullets = item.value.isNotEmpty() || item.bullets.isNotEmpty()

                    // Column 1: Serial Number
                    canvas.drawText("${item.number}.", leftX, tY, textPaint)

                    // Column 2: Label Lines
                    var labelY = tY
                    wt.labelLines.forEach { line ->
                        canvas.drawText(line, leftX + col1Width, labelY, textPaint)
                        labelY += 10f
                    }

                    if (hasValueOrBullets) {
                        // Column 3: Separator (:)
                        canvas.drawText(":", leftX + col1Width + col2Width + 5f, tY, textPaint)

                        // Column 4: Value or Bullets
                        if (item.value.isNotEmpty()) {
                            var valY = tY
                            wt.valLines.forEach { line ->
                                canvas.drawText(line, leftX + col1Width + col2Width + col3Width, valY, textPaint)
                                valY += 10f
                            }
                        } else if (item.bullets.isNotEmpty()) {
                            var currentBulletY = tY
                            item.bullets.forEachIndexed { bIdx, bullet ->
                                val bWrapped = wt.bulletsWrapped[bIdx]
                                canvas.drawText("•", leftX + col1Width + col2Width + col3Width - 6f, currentBulletY, textPaint)
                                bWrapped.forEach { line ->
                                    canvas.drawText(line, leftX + col1Width + col2Width + col3Width, currentBulletY, textPaint)
                                    currentBulletY += 10f
                                }
                                currentBulletY += 2f
                            }
                        }
                    }
                }
                engine.currentY += wt.itemH + termSpacing
            } else {
                // For simplicity, if a single term exceeds a page, we just chunk its bullets or valLines
                // We'll just split it into multiple term blocks without numbers.
                var remainingValLines = wt.valLines
                var remainingBullets = wt.bulletsWrapped
                
                var labelDrawn = false
                var numberDrawn = false
                
                // Extremely rare for a single term to span a page without bullets/valLines.
                // Just fallback to drawing as one and let it clip if it's crazy.
                engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)
                val tY = engine.currentY + 7f
                engine.addCommand { canvas, _, _ ->
                    val textPaint = engine.getPaint(COLOR_DARK_SLATE, 7f, TYPEFACE_NORMAL)
                    canvas.drawText("${item.number}.", leftX, tY, textPaint)
                    var labelY = tY
                    wt.labelLines.forEach { line ->
                        canvas.drawText(line, leftX + col1Width, labelY, textPaint)
                        labelY += 10f
                    }
                    if (item.value.isNotEmpty() || item.bullets.isNotEmpty()) {
                        canvas.drawText(":", leftX + col1Width + col2Width + 5f, tY, textPaint)
                        if (item.value.isNotEmpty()) {
                            var valY = tY
                            wt.valLines.forEach { line ->
                                canvas.drawText(line, leftX + col1Width + col2Width + col3Width, valY, textPaint)
                                valY += 10f
                            }
                        } else if (item.bullets.isNotEmpty()) {
                            var currentBulletY = tY
                            item.bullets.forEachIndexed { bIdx, bullet ->
                                val bWrapped = wt.bulletsWrapped[bIdx]
                                canvas.drawText("•", leftX + col1Width + col2Width + col3Width - 6f, currentBulletY, textPaint)
                                bWrapped.forEach { line ->
                                    canvas.drawText(line, leftX + col1Width + col2Width + col3Width, currentBulletY, textPaint)
                                    currentBulletY += 10f
                                }
                                currentBulletY += 2f
                            }
                        }
                    }
                }
                engine.currentY += wt.itemH + termSpacing
            }
        }
"""

if pattern.search(content):
    content = pattern.sub(replace_block, content, count=1)
    with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
        f.write(content)
    print("Replaced terms block.")
else:
    print("Could not find terms block.")
