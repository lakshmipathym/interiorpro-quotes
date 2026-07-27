        showQrCode: Boolean
    ) {
        val rowH = 16f
        val totalsRows = mutableListOf<Pair<String, String>>()
        totalsRows.add(Pair("Sub Total", "₹ " + formatIndianCurrency(quotation.subtotal)))
        if (quotation.discount > 0.0) {
            totalsRows.add(Pair("Discount", "₹ " + formatIndianCurrency(quotation.discount)))
        }
        if (showGst && (quotation.gstAmount > 0.0 || quotation.gstRate > 0.0)) {
            totalsRows.add(Pair("GST (${quotation.gstRate}%)", "₹ " + formatIndianCurrency(quotation.gstAmount)))
        }
        if (quotation.transport > 0.0) {
            totalsRows.add(Pair("Transport", "₹ " + formatIndianCurrency(quotation.transport)))
        }
        if (quotation.installation > 0.0) {
            totalsRows.add(Pair("Installation", "₹ " + formatIndianCurrency(quotation.installation)))
        }
        if (quotation.extraCharges > 0.0) {
            totalsRows.add(Pair("Extra Charges", "₹ " + formatIndianCurrency(quotation.extraCharges)))
        }
        if (Math.abs(quotation.roundOff) > 0.001) {
            totalsRows.add(Pair("Round Off", "₹ " + formatIndianCurrency(quotation.roundOff)))
        }

        val boxTopPadding = 10f
        val boxBottomPadding = 10f
        val hasAdvance = quotation.advance > 0.0
        val grandTotalH = if (hasAdvance) 50f else 22f
        val totalsBoxH = boxTopPadding + totalsRows.size * rowH + boxBottomPadding + grandTotalH

        val sectionSpacing = 14f
        var wordsH = 0f
        var wrappedWords: List<String> = emptyList()
        val wordsPaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD)
        
        val rawGrandTotal = quotation.subtotal - quotation.discount + quotation.gstAmount + quotation.transport + quotation.installation + quotation.extraCharges
        val calculatedGrandTotal = Math.round(rawGrandTotal).toDouble()
        val normalizedFinalGrandTotal = Math.round(calculatedGrandTotal * 100.0) / 100.0
        val balanceDue = normalizedFinalGrandTotal - quotation.advance
        if (showAmountInWords) {
            val currencyWord = if (Math.abs(normalizedFinalGrandTotal - 1.0) < 0.005) "Rupee " else "Rupees "
            val wordsStr = "Amount in Words: " + currencyWord + convertNumberToWords(normalizedFinalGrandTotal)
            wrappedWords = engine.wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)
            wordsH = wrappedWords.size * 10f + 14f
        }

        val hasBank = showBankDetails && company.bankName.trim().isNotBlank() && company.accountNumber.trim().isNotBlank()
        val hasQr = showQrCode && company.upiId.trim().isNotBlank()
        var bankRowCount = 0
        if (hasBank) {
            if (company.bankName.isNotBlank()) bankRowCount++
            if (company.accountHolderName.ifBlank { company.canonicalName }.isNotBlank()) bankRowCount++
            if (company.accountNumber.isNotBlank()) bankRowCount++
            if (company.ifsc.isNotBlank()) bankRowCount++
            if (company.branch.isNotBlank()) bankRowCount++
        }
        if (hasQr && company.upiId.isNotBlank()) bankRowCount++
        val payCardH = if (hasBank || hasQr) maxOf(if (hasQr) 65f else 0f, 28f + bankRowCount * 10.5f) else 0f

        val requiredHeight = wordsH + maxOf(totalsBoxH, payCardH)
        engine.ensureSpace(requiredHeight, reserveHeader = true)

        val wordsTop = engine.currentY
        val blockTop = wordsTop + wordsH
        val rightX = engine.marginX + 295f
        val leftX = engine.marginX
        val hasQrWidth = showQrCode && company.upiId.trim().isNotBlank()
        val leftW = if (hasQrWidth) 245f else 170f

        engine.addCommand { canvas, _, _ ->
            if (showAmountInWords) {
                var textY = wordsTop + 10f
                wrappedWords.forEach { line ->
                    canvas.drawText(line, engine.marginX, textY, wordsPaint)
                    textY += 10f
                }
            }

            // Draw Right Side Totals Box
            val boxPaint = engine.getPaint(COLOR_LIGHT_BG)
            val borderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.75f, style = Paint.Style.STROKE)
            val boxRect = RectF(rightX, blockTop, engine.endX, blockTop + totalsBoxH)
            canvas.drawRoundRect(boxRect, 4f, 4f, boxPaint)
            canvas.drawRoundRect(boxRect, 4f, 4f, borderPaint)

            val grandTotalY = blockTop + boxTopPadding + totalsRows.size * rowH + boxBottomPadding
            canvas.drawLine(rightX, grandTotalY, engine.endX, grandTotalY, borderPaint)
            
            val gtBgPaint = engine.getPaint(COLOR_PRIMARY_BLUE)
            val clipPath = android.graphics.Path()
            clipPath.addRoundRect(boxRect, 4f, 4f, android.graphics.Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRect(rightX, grandTotalY, engine.endX, grandTotalY + grandTotalH, gtBgPaint)
            canvas.restore()

            val labelPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_NORMAL)
            val valuePaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)
            val colonPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_NORMAL)

            totalsRows.forEachIndexed { idx, row ->
                val y = blockTop + boxTopPadding + idx * rowH
                canvas.drawText(row.first, rightX + 10f, y + 11f, labelPaint)
                canvas.drawText(":", rightX + 110f, y + 11f, colonPaint)
                canvas.drawText(row.second, engine.endX - 10f, y + 11f, valuePaint)
            }

            val textWhiteBold8 = engine.getPaint(COLOR_WHITE, 8f, TYPEFACE_BOLD)
            val textWhiteBold8_5Right = engine.getPaint(COLOR_WHITE, 8.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)

            canvas.drawText("Grand Total", rightX + 10f, grandTotalY + 14f, textWhiteBold8)
            canvas.drawText(":", rightX + 110f, grandTotalY + 14f, textWhiteBold8)
            canvas.drawText("₹ " + formatIndianCurrency(normalizedFinalGrandTotal), engine.endX - 10f, grandTotalY + 14f, textWhiteBold8_5Right)

            if (quotation.advance > 0.0) {
                val advanceY = grandTotalY + 28f
                canvas.drawText("Advance Paid", rightX + 10f, advanceY, textWhiteBold8)
                canvas.drawText(":", rightX + 110f, advanceY, textWhiteBold8)
                canvas.drawText("₹ " + formatIndianCurrency(quotation.advance), engine.endX - 10f, advanceY, textWhiteBold8_5Right)
                
                val balanceY = advanceY + 14f
                canvas.drawText("Balance Due", rightX + 10f, balanceY, textWhiteBold8)
                canvas.drawText(":", rightX + 110f, balanceY, textWhiteBold8)
                canvas.drawText("₹ " + formatIndianCurrency(balanceDue), engine.endX - 10f, balanceY, textWhiteBold8_5Right)
            }

            if (hasBank || hasQr) {
                val payCardTop = blockTop
                val payCard = RectF(leftX, payCardTop, leftX + leftW, payCardTop + payCardH)
                canvas.drawRoundRect(payCard, 4f, 4f, engine.getPaint(COLOR_LIGHT_BG))
                canvas.drawRoundRect(payCard, 4f, 4f, engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE))

                canvas.drawText("PAYMENT METHOD", leftX + 8f, blockTop + 12f, engine.getPaint(COLOR_PRIMARY_BLUE, 7.5f, TYPEFACE_BOLD))

                var bY = payCardTop + 24f
                val pLabelP = engine.getPaint(COLOR_TEXT_SECONDARY, 7f, TYPEFACE_NORMAL)
                val pValP = engine.getPaint(COLOR_DARK_SLATE, 7f, TYPEFACE_BOLD)

                fun drawBankRow(label: String, value: String) {
                    if (value.isNotBlank()) {
                        canvas.drawText(label, leftX + 10f, bY, pLabelP)
                        canvas.drawText(":", leftX + 85f, bY, pValP)
                        canvas.drawText(value, leftX + 92f, bY, pValP)
                        bY += 10.5f
                    }
                }

                if (hasBank) {
                    drawBankRow("Bank Name", company.bankName)
                    drawBankRow("A/c Holder", company.accountHolderName.ifBlank { company.canonicalName })
                    drawBankRow("A/c Number", company.accountNumber)
                    drawBankRow("IFSC Code", company.ifsc)
                    drawBankRow("Branch", company.branch)
                    if (hasQr && company.upiId.isNotBlank()) {
                        drawBankRow("UPI ID", company.upiId)
                    }
                } else if (hasQr) {
                    val textY = payCardTop + (payCardH / 2f) + 2.5f
                    canvas.drawText("UPI ID", leftX + 10f, textY, pLabelP)
                    canvas.drawText(":", leftX + 85f, textY, pValP)
                    canvas.drawText(company.upiId, leftX + 92f, textY, pValP)
                }

                if (hasQr) {
                    val qrSize = payCardH - 12f
                    val qrLeft = leftX + leftW - qrSize - 8f
                    val qrTop = payCardTop + (payCardH - qrSize) / 2f
                    fun uriEncodeSafely(text: String): String {
                        return try {
                            java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20")
                        } catch (e: Exception) {
                            text.replace(" ", "%20")
                        }
                    }
                    val upiPayload = "upi://pay?pa=${company.upiId}&pn=${uriEncodeSafely(company.canonicalName)}"
                    drawQrCode(engine, canvas, qrLeft, qrTop, qrSize, upiPayload)
                }
            }
        }
        engine.currentY = blockTop + maxOf(totalsBoxH, payCardH) + sectionSpacing
    }

    private fun drawTerms(
        engine: PdfEngine,
        company: CompanyProfile,
        quotation: Quotation,
        showTermsConditions: Boolean
    ) {
        if (!showTermsConditions) return

        val termsList = mutableListOf<String>()

        // Dynamic terms settings values
        val warrantyVal = quotation.warranty.ifBlank { company.defaultWarranty }
        if (warrantyVal.isNotBlank()) {
            termsList.add("Warranty : $warrantyVal")
        }
        if (company.defaultDeliveryTime.isNotBlank()) {
            termsList.add("Delivery Time : ${company.defaultDeliveryTime}")
        }
        if (company.defaultInstallationTime.isNotBlank()) {
            termsList.add("Installation Time : ${company.defaultInstallationTime}")
        }
        if (company.defaultPaymentTerms.isNotBlank()) {
            termsList.add("Payment Terms : ${company.defaultPaymentTerms}")
        }
        val validityDays = quotation.validityDays.takeIf { it > 0 } ?: company.defaultValidityDays.takeIf { it > 0 } ?: 30
        termsList.add("Quote Validity : $validityDays Days")
        if (company.additionalConditions.isNotBlank()) {
            termsList.add("Additional Conditions : ${company.additionalConditions}")
        }

        val rawTerms = quotation.termsAndConditions.trim()
        if (rawTerms.isNotBlank()) {
            rawTerms.split("\n").forEach { term ->
                val trimmed = term.trim()
                if (trimmed.isNotBlank()) {
                    termsList.add(trimmed)
                }
            }
        }

        if (termsList.isEmpty()) return
        val measurePaint = engine.getPaint(COLOR_DARK_SLATE, 7f, TYPEFACE_NORMAL)

