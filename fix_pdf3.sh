cat << 'INNEREOF' > patch.py
import re

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "r") as f:
    content = f.read()

content = content.replace("""        engine.currentY += totalsBoxH
        if (showAmountInWords) {
            val wordsStr = "Amount in Words: Rupee " + convertNumberToWords(quotation.grandTotal) + " Only"
            val wordsPaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD)
            val wrappedWords = engine.wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)
            val wordsH = wrappedWords.size * 10f
            engine.ensureSpace(wordsH, reserveHeader = true)
            val wTop = engine.currentY
            engine.addCommand { canvas, _, _ ->
                var textY = wTop + 8f
                wrappedWords.forEach { line ->
                    canvas.drawText(line, engine.marginX, textY, wordsPaint)
                    textY += 10f
                }
            }
            engine.currentY += wordsH + 8f + sectionSpacing
        }""", """        engine.currentY += totalsBoxH
        if (showAmountInWords) {
            engine.currentY += 8f
            val wordsStr = "Amount in Words: Rupee " + convertNumberToWords(quotation.grandTotal) + " Only"
            val wordsPaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD)
            val wrappedWords = engine.wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)
            val wordsH = wrappedWords.size * 10f
            engine.ensureSpace(wordsH, reserveHeader = true)
            val wTop = engine.currentY
            engine.addCommand { canvas, _, _ ->
                var textY = wTop + 8f
                wrappedWords.forEach { line ->
                    canvas.drawText(line, engine.marginX, textY, wordsPaint)
                    textY += 10f
                }
            }
            engine.currentY += wordsH + sectionSpacing
        } else {
            engine.currentY += sectionSpacing
        }""")

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
    f.write(content)

INNEREOF
python3 patch.py
