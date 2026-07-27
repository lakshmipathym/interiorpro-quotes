import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# 1. Canonical company name
content = content.replace("val companyNameText = company.companyName", "val companyNameText = company.companyName.trim().ifBlank { \"Company Name\" }")
content = content.replace("company.companyName.ifBlank { \"Quotation\" }", "company.companyName.trim().ifBlank { \"Company Name\" }")
content = content.replace("company.accountHolderName.ifBlank { company.companyName }", "company.accountHolderName.ifBlank { company.companyName.trim().ifBlank { \"Company Name\" } }")
content = content.replace("company.companyName.uppercase()", "company.companyName.trim().ifBlank { \"Company Name\" }.uppercase()")
content = content.replace("${uriEncodeSafely(company.companyName)}", "${uriEncodeSafely(company.companyName.trim().ifBlank { \"Company Name\" })}")
content = content.replace("val coNameVal = company.companyName", "val coNameVal = company.companyName.trim().ifBlank { \"Company Name\" }")
content = content.replace("company.companyName.isNotBlank()", "true")

# 2. Dimensions
old_dims = """            val dims = mutableListOf<String>()
            if (wFeet > 0.0) dims.add(specs.width.trim())
            if (hFeet > 0.0) dims.add(specs.height.trim())
            if (dFeet > 0.0) dims.add(specs.depth.trim())"""

new_dims = """            val dims = mutableListOf<String>()
            fun formatDimension(feet: Double): String {
                if (feet <= 0.0) return ""
                val wholeFeet = feet.toLong()
                val inches = Math.round((feet - wholeFeet) * 12.0)
                var f = wholeFeet
                var i = inches
                if (i == 12L) { f += 1; i = 0 }
                return if (i == 0L) "$f'" else if (f == 0L) "$i\\"" else "$f' $i\\""
            }
            if (wFeet > 0.0) dims.add(formatDimension(wFeet))
            if (hFeet > 0.0) dims.add(formatDimension(hFeet))
            if (dFeet > 0.0) dims.add(formatDimension(dFeet))"""
content = content.replace(old_dims, new_dims)

# 3. Billable display
old_billable = """            val billableLabel = when {
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

new_billable = """            val billableLabel = when {
                uLower.contains("sq") -> "Billable Area :"
                uLower.contains("cu") -> "Billable Volume :"
                uLower.contains("ft") || uLower.contains("meter") || uLower == "r.m" || uLower == "rm" -> "Running Length :"
                else -> ""
            }
            if (billableLabel.isNotEmpty()) {
                if (sizeLinesStr.isNotEmpty()) sizeLinesStr.add("") // empty line for spacing
                
                val qtyRounded = Math.round(billableQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%.0f %s", qtyRounded, item.unit.trim()) else String.format(Locale.US, "%.2f %s", qtyRounded, item.unit.trim())
                
                val combinedLine = "$billableLabel $qtyStr"
                val lines = engine.wrapText(combinedLine, (colWidths[3] - 4f).toInt(), engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD))
                sizeLinesStr.addAll(lines)
            }"""
content = content.replace(old_billable, new_billable)

# 4. Size drawing logic
old_size_draw = """                sizeLinesStr.forEach { line ->
                    if (line == "Size") {
                        canvas.drawText(line, col3CenterX, sizeY, sizeLabelPaint)
                    } else if (line.startsWith("Billable") || line.startsWith("Running Length")) {
                        val paint = engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER)
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    } else if (line.isNotBlank()) {
                        val unitStr = item.unit.trim()
                        val paint = if (unitStr.isNotEmpty() && line.contains(unitStr)) engine.getPaint(COLOR_DARK_SLATE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    }
                    sizeY += 10.5f
                }"""

new_size_draw = """                sizeLinesStr.forEach { line ->
                    if (line == "Size") {
                        canvas.drawText(line, col3CenterX, sizeY, sizeLabelPaint)
                    } else if (line.isNotBlank()) {
                        val paint = if (line.contains("Billable") || line.contains("Running")) engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER) else sizeValuePaint
                        canvas.drawText(line, col3CenterX, sizeY, paint)
                    }
                    sizeY += 10.5f
                }"""
content = content.replace(old_size_draw, new_size_draw)

# 5. Amount in words single source of truth
old_words = """        if (showAmountInWords) {
            val currencyWord = if (Math.abs(quotation.grandTotal - 1.0) < 0.005) "Rupee " else "Rupees "
            val wordsStr = "Amount in Words: " + currencyWord + convertNumberToWords(quotation.grandTotal)
            wrappedWords = engine.wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)
            wordsH = wrappedWords.size * 10f + 14f
        }"""
        
new_words = """        val normalizedFinalGrandTotal = Math.round(quotation.grandTotal * 100.0) / 100.0
        if (showAmountInWords) {
            val currencyWord = if (Math.abs(normalizedFinalGrandTotal - 1.0) < 0.005) "Rupee " else "Rupees "
            val wordsStr = "Amount in Words: " + currencyWord + convertNumberToWords(normalizedFinalGrandTotal)
            wrappedWords = engine.wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)
            wordsH = wrappedWords.size * 10f + 14f
        }"""
content = content.replace(old_words, new_words)

old_grand = 'canvas.drawText("₹ " + formatIndianCurrency(quotation.grandTotal), engine.endX - 10f, grandTotalY + 14f, textWhiteBold8_5Right)'
new_grand = 'canvas.drawText("₹ " + formatIndianCurrency(normalizedFinalGrandTotal), engine.endX - 10f, grandTotalY + 14f, textWhiteBold8_5Right)'
content = content.replace(old_grand, new_grand)

# 6. Terms & Conditions Title wrapping logic
# It was updated to be drawn dynamically if not titleDrawn, which is correct.
# Let's ensure term item logic isn't split awkwardly. Wait, this was already fixed in previous script.

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
