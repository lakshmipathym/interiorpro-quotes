sed -i '873,934c\
        // Amount in Words calculation\
        val sectionSpacing = 14f\
        engine.ensureSpace(totalsBoxH + sectionSpacing, reserveHeader = true)\
        val blockTop = engine.currentY\
        val rightX = engine.marginX + 295f\
        engine.addCommand { canvas, _, _ ->\
            // Draw Right Side Totals Box\
            val boxPaint = engine.getPaint(COLOR_LIGHT_BG)\
            val borderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.75f, style = Paint.Style.STROKE)\
            val boxRect = RectF(rightX, blockTop, engine.endX, blockTop + totalsBoxH)\
            canvas.drawRoundRect(boxRect, 4f, 4f, boxPaint)\
            canvas.drawRoundRect(boxRect, 4f, 4f, borderPaint)\
            val grandTotalY = blockTop + boxTopPadding + totalsRows.size * rowH + boxBottomPadding\
            canvas.drawLine(rightX, grandTotalY, engine.endX, grandTotalY, borderPaint)\
            val gtBgPaint = engine.getPaint(COLOR_PRIMARY_BLUE)\
            val clipPath = android.graphics.Path()\
            clipPath.addRoundRect(boxRect, 4f, 4f, android.graphics.Path.Direction.CW)\
            canvas.save()\
            canvas.clipPath(clipPath)\
            canvas.drawRect(rightX, grandTotalY, engine.endX, grandTotalY + grandTotalH, gtBgPaint)\
            canvas.restore()\
            val labelPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_NORMAL)\
            val valuePaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)\
            val colonPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_NORMAL)\
            totalsRows.forEachIndexed { idx, row ->\
                val y = blockTop + boxTopPadding + idx * rowH\
                canvas.drawText(row.first, rightX + 10f, y + 11f, labelPaint)\
                canvas.drawText(":", rightX + 110f, y + 11f, colonPaint)\
                canvas.drawText(row.second, engine.endX - 10f, y + 11f, valuePaint)\
            }\
            val textWhiteBold8 = engine.getPaint(COLOR_WHITE, 8f, TYPEFACE_BOLD)\
            val textWhiteBold8_5Right = engine.getPaint(COLOR_WHITE, 8.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)\
            canvas.drawText("GRAND TOTAL", rightX + 10f, grandTotalY + 14f, textWhiteBold8)\
            canvas.drawText(":", rightX + 110f, grandTotalY + 14f, textWhiteBold8)\
            canvas.drawText("₹ " + formatIndianCurrency(quotation.grandTotal), engine.endX - 10f, grandTotalY + 14f, textWhiteBold8_5Right)\
        }\
        engine.currentY += totalsBoxH + 8f\
\
        if (showAmountInWords) {\
            val wordsStr = "Amount in Words: Rupee " + convertNumberToWords(quotation.grandTotal) + " Only"\
            val wordsPaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD)\
            val wrappedWords = engine.wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)\
            val wordsH = wrappedWords.size * 10f\
            engine.ensureSpace(wordsH + sectionSpacing, reserveHeader = true)\
            val wTop = engine.currentY\
            engine.addCommand { canvas, _, _ ->\
                var textY = wTop + 8f\
                wrappedWords.forEach { line ->\
                    canvas.drawText(line, engine.marginX, textY, wordsPaint)\
                    textY += 10f\
                }\
            }\
            engine.currentY += wordsH + sectionSpacing\
        }' app/src/main/java/com/example/pdf/PdfGenerator.kt
