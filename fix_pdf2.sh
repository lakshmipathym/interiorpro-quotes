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

content = content.replace("engine.ensureSpace(payCardH + sectionSpacing, reserveHeader = true)", "engine.ensureSpace(payCardH, reserveHeader = true)")

content = content.replace("""        val totalTermsSectionHeight = 22f + contentHeight
        val pageCapacity = engine.maxContentY - engine.topMargin
        if (totalTermsSectionHeight <= pageCapacity) {
            engine.ensureSpace(totalTermsSectionHeight, reserveHeader = true)
        } else {
            val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
            engine.ensureSpace(22f + firstTermH + termSpacing, reserveHeader = true)
        }""", """        val totalTermsSectionHeight = 22f + contentHeight
        val pageCapacity = engine.maxContentY - engine.topMargin
        val remainingSpace = engine.maxContentY - engine.currentY
        if (totalTermsSectionHeight <= remainingSpace) {
            // Fits on current page, no explicit break needed
        } else if (totalTermsSectionHeight <= pageCapacity) {
            // Move as one complete section to next page
            engine.ensureSpace(totalTermsSectionHeight, reserveHeader = true)
        } else {
            // Split it if it doesn't fit on a blank page
            val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
            engine.ensureSpace(22f + firstTermH, reserveHeader = true)
        }""")

content = content.replace("""        val signatureBoxH = 70f
        engine.ensureSpace(signatureBoxH + sectionSpacing, reserveHeader = true)
        engine.currentY += sectionSpacing""", """        val signatureBoxH = 70f
        engine.ensureSpace(signatureBoxH, reserveHeader = true)""")

with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
    f.write(content)

INNEREOF
python3 patch.py
