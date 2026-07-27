import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Replace Terms & Conditions rendering logic
old_terms = """        val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
        engine.ensureSpace(titleHeight + firstTermH, reserveHeader = true)

        val blockTop = engine.currentY
        engine.addCommand { canvas, _, _ ->
            val titlePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 8.5f, TYPEFACE_BOLD)
            canvas.drawText("TERMS & CONDITIONS", leftX, blockTop + 8f, titlePaint)
        }
        engine.currentY += 12f

        wrappedTerms.forEachIndexed { index, wt ->
            val item = wt.item
            engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)
            val tY = engine.currentY
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
                        wt.bulletsWrapped.forEach { bLines ->
                            // Draw Bullet Symbol
                            canvas.drawText("•", leftX + col1Width + col2Width + col3Width, currentBulletY, textPaint)
                            // Draw Bullet Lines (indented slightly)
                            var bY = currentBulletY
                            bLines.forEach { line ->
                                canvas.drawText(line, leftX + col1Width + col2Width + col3Width + 10f, bY, textPaint)
                                bY += 10f
                            }
                            currentBulletY = bY
                        }
                    }
                }
            }
            engine.currentY += wt.itemH + termSpacing
        }
        engine.currentY -= termSpacing // remove trailing term gap
        engine.currentY += sectionSpacing // add section gap"""

new_terms = """        val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
        engine.ensureSpace(titleHeight + firstTermH + termSpacing, reserveHeader = true)

        var blockTop = engine.currentY
        engine.addCommand { canvas, _, _ ->
            val titlePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 8.5f, TYPEFACE_BOLD)
            canvas.drawText("TERMS & CONDITIONS", leftX, blockTop + 8f, titlePaint)
        }
        engine.currentY += 18f

        wrappedTerms.forEachIndexed { index, wt ->
            val item = wt.item
            if (index > 0) {
                engine.ensureSpace(wt.itemH + termSpacing, reserveHeader = true)
            }
            val tY = engine.currentY + 8f // Offset baseline down
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
                        wt.bulletsWrapped.forEach { bLines ->
                            // Draw Bullet Symbol
                            canvas.drawText("•", leftX + col1Width + col2Width + col3Width, currentBulletY, textPaint)
                            // Draw Bullet Lines (indented slightly)
                            var bY = currentBulletY
                            bLines.forEach { line ->
                                canvas.drawText(line, leftX + col1Width + col2Width + col3Width + 10f, bY, textPaint)
                                bY += 10f
                            }
                            currentBulletY = bY
                        }
                    }
                }
            }
            engine.currentY += wt.itemH + termSpacing
        }
        engine.currentY += sectionSpacing // add section gap"""

if old_terms in content:
    content = content.replace(old_terms, new_terms)
else:
    print("Could not patch terms")

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
