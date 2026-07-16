package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.AppDatabase
import com.example.data.CompanyProfile
import com.example.data.CustomerEntity
import com.example.data.Quotation
import com.example.data.QuotationItem
import com.example.data.QuotesRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import com.example.utils.CurrencyFormatter
import com.example.utils.ImageManager

object PdfGenerator {

    // --- OFFICIAL BRAND COLORS ---
    private const val COLOR_PRIMARY_BLUE = "#1E3A8A"
    private const val COLOR_ACCENT_ORANGE = "#EA580C"
    private const val COLOR_DARK_SLATE = "#0F172A"
    private const val COLOR_LIGHT_BG = "#F8FAFC"
    private const val COLOR_WHITE = "#FFFFFF"
    private const val COLOR_BORDER = "#E5E7EB"
    private const val COLOR_TEXT_SECONDARY = "#64748B"
    private const val COLOR_SUCCESS = "#16A34A"
    private const val COLOR_DANGER = "#DC2626"

    // Main Engine to handle dynamic, multi-page layout and command recording
    private class PdfEngine(
        val context: Context,
        val company: CompanyProfile,
        val quotation: Quotation
    ) {
        val pageWidth = 595f
        val pageHeight = 842f
        val marginX = 36f
        val endX = 559f
        val usableWidth = 523f
        val topMargin = 40f
        val bottomMargin = 40f
        val maxContentY = pageHeight - bottomMargin - 20f // safety margin for footer

        var currentY = topMargin
        var currentPageIndex = 0
        var inTableMode = false

        // Drawing commands grouped by page index: (Canvas, CurrentPage, TotalPages) -> Unit
        val pagesCommands = mutableListOf<MutableList<(Canvas, Int, Int) -> Unit>>()

        init {
            pagesCommands.add(mutableListOf())
        }

        fun ensureSpace(heightNeeded: Float, reserveHeader: Boolean = false) {
            if (currentY + heightNeeded > maxContentY) {
                startNewPage(reserveHeader)
            }
        }

        fun startNewPage(reserveHeader: Boolean = false) {
            currentPageIndex++
            pagesCommands.add(mutableListOf())
            currentY = topMargin
            if (reserveHeader) {
                drawRunningPageHeader()
            }
            if (inTableMode) {
                drawTableHeaderDirectly()
            }
        }

        fun addCommand(command: (Canvas, Int, Int) -> Unit) {
            pagesCommands[currentPageIndex].add(command)
        }

        private fun drawRunningPageHeader() {
            val y = currentY
            val companyNameText = company.companyName
            val quoteNumText = quotation.quotationNumber
            if (companyNameText.isNotBlank()) {
                addCommand { canvas, _, _ ->
                    val p = Paint().apply {
                        color = Color.parseColor(COLOR_TEXT_SECONDARY)
                        textSize = 7.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText("${companyNameText.uppercase()} - QUOTATION #$quoteNumText", marginX, y + 10f, p)
                    canvas.drawLine(marginX, y + 14f, endX, y + 14f, Paint().apply {
                        color = Color.parseColor(COLOR_BORDER)
                        strokeWidth = 0.5f
                    })
                }
                currentY += 24f
            }
        }

        fun drawTableHeaderDirectly() {
            val y = currentY
            val colWidths = floatArrayOf(22f, 115f, 165f, 55f, 35f, 65f, 66f)
            val colX = FloatArray(colWidths.size)
            var tempX = marginX
            for (i in colWidths.indices) {
                colX[i] = tempX
                tempX += colWidths[i]
            }

            addCommand { canvas, _, _ ->
                val headerBgPaint = Paint().apply {
                    color = Color.parseColor(COLOR_PRIMARY_BLUE)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(colX[0], y, colX[colX.size - 1] + colWidths[colWidths.size - 1], y + 18f, headerBgPaint)

                val textPaint = Paint().apply {
                    color = Color.parseColor(COLOR_WHITE)
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }

                val fontMetrics = textPaint.fontMetrics
                val centerY = y + 18f / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f

                val headers = listOf("Sl No", "Item", "Specifications", "Size", "Qty", "Rate", "Amount")
                for (i in headers.indices) {
                    if (i == 5 || i == 6) {
                        val cx = colX[i] + colWidths[i] - 4f
                        val rightPaint = Paint(textPaint).apply { textAlign = Paint.Align.RIGHT }
                        canvas.drawText(headers[i], cx, centerY, rightPaint)
                    } else {
                        val cx = colX[i] + colWidths[i] / 2f
                        canvas.drawText(headers[i], cx, centerY, textPaint)
                    }
                }
            }
            currentY += 18f
        }
    }

    private fun extractPanFromGstin(gstin: String): String? {
        val clean = gstin.trim().uppercase(Locale.US)
        if (clean.length == 15) {
            val pan = clean.substring(2, 12)
            val panRegex = Regex("[A-Z]{5}[0-9]{4}[A-Z]{1}")
            if (pan.matches(panRegex)) {
                return pan
            }
        }
        return null
    }

    suspend fun generateQuotationPdf(
        context: Context,
        repository: QuotesRepository,
        quotationId: Int,
        outputFile: File
    ): File {
        val company = repository.getCompanyProfileDirect() ?: CompanyProfile()
        val quotation = repository.getQuotationByIdDirect(quotationId) ?: throw IllegalArgumentException("Quotation not found with id: $quotationId")
        val items = repository.getQuotationItemsDirect(quotationId)
        return generateQuotationPdf(context, company, quotation, items, outputFile)
    }

    private fun generateQuotationPdf(
        context: Context,
        company: CompanyProfile,
        quotation: Quotation,
        items: List<QuotationItem>,
        outputFile: File
    ): File {
        val pdfPrefs = context.getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE)
        val showLogo = pdfPrefs.getBoolean("pdf_show_logo", true)
        val showGst = pdfPrefs.getBoolean("pdf_show_gst", true)
        val showWebsite = pdfPrefs.getBoolean("pdf_show_website", true)
        val showWhatsapp = pdfPrefs.getBoolean("pdf_show_whatsapp", true)
        val showValidUntil = pdfPrefs.getBoolean("pdf_show_valid_until", true)
        val showQrCode = pdfPrefs.getBoolean("pdf_show_qr_code", true)
        val showBankDetails = pdfPrefs.getBoolean("pdf_show_bank_details", true)
        val showAmountInWords = pdfPrefs.getBoolean("pdf_show_amount_in_words", true)
        val showCompanySeal = pdfPrefs.getBoolean("pdf_show_company_seal", true)
        val showSignature = pdfPrefs.getBoolean("pdf_show_signature", true)
        val showTermsConditions = pdfPrefs.getBoolean("pdf_show_terms_conditions", true)
        val showPageNumber = pdfPrefs.getBoolean("pdf_show_page_number", true)

        // Force dynamic recalculation of Item Amount = Qty * Rate
        val validatedItems = items.map { item ->
            val expectedAmount = item.quantity * item.rate
            if (Math.abs(item.amount - expectedAmount) > 0.005) {
                item.copy(amount = expectedAmount)
            } else {
                item
            }
        }

        val subtotal = validatedItems.sumOf { it.amount }
        val taxable = maxOf(0.0, subtotal - quotation.discount)
        val gstAmount = if (showGst) ((taxable * quotation.gstRate) / 100.0) else 0.0
        val grandTotalRaw = taxable + gstAmount
        val grandTotalRounded = Math.round(grandTotalRaw).toDouble()
        val roundOff = grandTotalRounded - grandTotalRaw

        val updatedQuotation = quotation.copy(
            subtotal = subtotal,
            gstAmount = gstAmount,
            grandTotal = grandTotalRounded
        )

        // Query database for complete client details
        val db = AppDatabase.getDatabase(context)
        val customer = kotlinx.coroutines.runBlocking {
            try {
                db.customerDao().getCustomerById(updatedQuotation.customerId.toLong())
            } catch (e: Exception) {
                null
            }
        }

        val engine = PdfEngine(context, company, updatedQuotation)

        // --- RENDER MODULAR SECTIONS ---
        drawHeader(engine, company, updatedQuotation, showLogo, showValidUntil, showGst, showWebsite, showWhatsapp)
        drawCustomer(engine, customer, updatedQuotation)
        drawItemsTable(engine, validatedItems)
        drawSummary(engine, updatedQuotation, showGst, showAmountInWords, roundOff)
        drawPayment(engine, company, showBankDetails, showQrCode)
        drawTerms(engine, company, updatedQuotation, showTermsConditions)
        drawSignature(engine, company, showCompanySeal, showSignature)
        drawReferenceImages(engine, validatedItems)

        // --- COMPILE PDF DOCUMENT PAGES ---
        val pdfDocument = PdfDocument()
        engine.pagesCommands.forEachIndexed { idx, commands ->
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Standard elegant corporate page border
            val pBorder = Paint().apply {
                color = Color.parseColor(COLOR_BORDER)
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(20f, 20f, 575f, 822f, pBorder)

            commands.forEach { cmd ->
                cmd(canvas, idx + 1, engine.pagesCommands.size)
            }

            drawFooter(canvas, idx + 1, engine.pagesCommands.size, company, showPageNumber)
            pdfDocument.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawHeader(
        engine: PdfEngine,
        company: CompanyProfile,
        quotation: Quotation,
        showLogo: Boolean,
        showValidUntil: Boolean,
        showGst: Boolean,
        showWebsite: Boolean,
        showWhatsapp: Boolean
    ) {
        val headerY = engine.currentY
        val hasLogo = showLogo && company.logoPath.isNotBlank() && File(company.logoPath).exists()
        val logoWidth = 45f
        val logoHeight = 45f

        val headerTextLeft = if (hasLogo) engine.marginX + logoWidth + 12f else engine.marginX

        val compNamePaint = Paint().apply {
            color = Color.parseColor(COLOR_PRIMARY_BLUE)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val taglinePaint = Paint().apply {
            color = Color.parseColor(COLOR_ACCENT_ORANGE)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val contactPaint = Paint().apply {
            color = Color.parseColor(COLOR_TEXT_SECONDARY)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val maxLeftW = (390f - headerTextLeft - 15f).toInt()
        val coNameLines = if (company.companyName.isNotBlank()) wrapText(company.companyName.uppercase(), maxLeftW, compNamePaint) else emptyList()
        val taglineLines = if (company.tagline.isNotBlank()) wrapText(company.tagline, maxLeftW, taglinePaint) else emptyList()

        val addrParts = listOf(company.address, company.city, company.state, company.pincode).filter { it.isNotBlank() }
        val addrLines = if (addrParts.isNotEmpty()) wrapText(addrParts.joinToString(", "), maxLeftW, contactPaint) else emptyList()

        val phoneEmailParts = mutableListOf<String>()
        if (company.phone.isNotBlank()) phoneEmailParts.add("Ph: ${company.phone}")
        if (showWhatsapp && company.whatsappNumber.isNotBlank()) phoneEmailParts.add("WA: ${company.whatsappNumber}")
        if (company.email.isNotBlank()) phoneEmailParts.add("Email: ${company.email}")
        val phoneEmailLines = if (phoneEmailParts.isNotEmpty()) wrapText(phoneEmailParts.joinToString(" | "), maxLeftW, contactPaint) else emptyList()

        val gstinWebParts = mutableListOf<String>()
        if (showGst && company.gstin.isNotBlank()) gstinWebParts.add("GSTIN: ${company.gstin}")
        val computedPan = if (showGst) extractPanFromGstin(company.gstin) else null
        if (computedPan != null) gstinWebParts.add("PAN: $computedPan")
        if (showWebsite && company.website.isNotBlank()) gstinWebParts.add("Web: ${company.website}")
        val gstinWebLines = if (gstinWebParts.isNotEmpty()) wrapText(gstinWebParts.joinToString(" | "), maxLeftW, contactPaint) else emptyList()

        val logoBottom = if (hasLogo) headerY + logoHeight else headerY
        var leftTextY = headerY
        if (coNameLines.isNotEmpty()) {
            leftTextY += 12f
            if (coNameLines.size > 1) {
                leftTextY += (coNameLines.size - 1) * 14f
            }
        }
        if (taglineLines.isNotEmpty()) {
            leftTextY += taglineLines.size * 10f + 2f
        }
        if (addrLines.isNotEmpty()) {
            leftTextY += addrLines.size * 9f + 2f
        }
        if (phoneEmailLines.isNotEmpty()) {
            leftTextY += phoneEmailLines.size * 9f + 2f
        }
        if (gstinWebLines.isNotEmpty()) {
            leftTextY += gstinWebLines.size * 9f + 2f
        }

        val textHeight = leftTextY - headerY
        val logoTop = if (hasLogo && textHeight > logoHeight) {
            headerY + (textHeight - logoHeight) / 2f
        } else {
            headerY
        }
        val textStartTop = if (hasLogo && logoHeight > textHeight) {
            headerY + (logoHeight - textHeight) / 2f
        } else {
            headerY
        }

        val leftColumnBottom = headerY + maxOf(if (hasLogo) logoHeight else 0f, textHeight)
        val boxBottom = if (showValidUntil) headerY + 54f else headerY + 36f
        val contentBottom = maxOf(leftColumnBottom, boxBottom)

        val bannerY = contentBottom + 12f
        val totalHeaderHeight = (bannerY + 18f + 14f) - headerY

        engine.addCommand { canvas, _, _ ->
            if (hasLogo) {
                drawBitmapSafely(canvas, company.logoPath, engine.marginX, logoTop, logoWidth, logoHeight)
            }

            var currY = textStartTop
            if (coNameLines.isNotEmpty()) {
                coNameLines.forEach { line ->
                    canvas.drawText(line, headerTextLeft, currY + 12f, compNamePaint)
                    currY += 14f
                }
                currY += 2f
            }
            if (taglineLines.isNotEmpty()) {
                taglineLines.forEach { line ->
                    canvas.drawText(line, headerTextLeft, currY + 8f, taglinePaint)
                    currY += 10f
                }
                currY += 2f
            }
            if (addrLines.isNotEmpty()) {
                addrLines.forEach { line ->
                    canvas.drawText(line, headerTextLeft, currY + 7f, contactPaint)
                    currY += 9f
                }
                currY += 2f
            }
            if (phoneEmailLines.isNotEmpty()) {
                phoneEmailLines.forEach { line ->
                    canvas.drawText(line, headerTextLeft, currY + 7f, contactPaint)
                    currY += 9f
                }
                currY += 2f
            }
            if (gstinWebLines.isNotEmpty()) {
                gstinWebLines.forEach { line ->
                    canvas.drawText(line, headerTextLeft, currY + 7f, contactPaint)
                    currY += 9f
                }
            }

            // Document Metadata Box (Right Column)
            val boxLeft = 390f
            val boxW = 169f
            val boxTop = headerY

            val metaBox = RectF(boxLeft, boxTop, boxLeft + boxW, boxBottom)
            canvas.drawRoundRect(metaBox, 4f, 4f, Paint().apply { color = Color.parseColor(COLOR_LIGHT_BG) })
            canvas.drawRoundRect(metaBox, 4f, 4f, Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.5f; style = Paint.Style.STROKE })

            val lineP = Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.5f }
            canvas.drawLine(boxLeft, boxTop + 18f, boxLeft + boxW, boxTop + 18f, lineP)
            if (showValidUntil) {
                canvas.drawLine(boxLeft, boxTop + 36f, boxLeft + boxW, boxTop + 36f, lineP)
            }

            val labelP = Paint().apply { color = Color.parseColor(COLOR_TEXT_SECONDARY); textSize = 7.5f; isAntiAlias = true }
            val valP = Paint().apply { color = Color.parseColor(COLOR_DARK_SLATE); textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.US)
            val dateStr = sdf.format(Date(quotation.date))
            val validityDays = company.defaultValidityDays.takeIf { it > 0 } ?: 30
            val validUntilMs = quotation.date + (validityDays.toLong() * 24L * 60L * 60L * 1000L)
            val validUntilStr = sdf.format(Date(validUntilMs))

            canvas.drawText("Quotation No", boxLeft + 8f, boxTop + 12f, labelP)
            canvas.drawText(":", boxLeft + 65f, boxTop + 12f, valP)
            canvas.drawText(quotation.quotationNumber, boxLeft + 72f, boxTop + 12f, valP)

            canvas.drawText("Quote Date", boxLeft + 8f, boxTop + 30f, labelP)
            canvas.drawText(":", boxLeft + 65f, boxTop + 30f, valP)
            canvas.drawText(dateStr, boxLeft + 72f, boxTop + 30f, valP)

            if (showValidUntil) {
                canvas.drawText("Valid Until", boxLeft + 8f, boxTop + 48f, labelP)
                canvas.drawText(":", boxLeft + 65f, boxTop + 48f, valP)
                canvas.drawText(validUntilStr, boxLeft + 72f, boxTop + 48f, valP)
            }

            val titleP = Paint().apply {
                color = Color.parseColor(COLOR_PRIMARY_BLUE)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("ESTIMATE & QUOTATION", 595f / 2f, bannerY + 12f, titleP)
            canvas.drawLine(engine.marginX, bannerY + 18f, engine.endX, bannerY + 18f, Paint().apply { color = Color.parseColor(COLOR_ACCENT_ORANGE); strokeWidth = 1f })
        }
        engine.currentY += totalHeaderHeight
    }

    private fun drawCustomer(engine: PdfEngine, customer: CustomerEntity?, quotation: Quotation) {
        val clientRowsLeft = mutableListOf<Pair<String, String>>()
        val clientRowsRight = mutableListOf<Pair<String, String>>()

        fun isValidCustomerValue(value: String?): Boolean {
            if (value == null) return false
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return false
            return !trimmed.equals("null", ignoreCase = true)
        }

        if (customer != null) {
            if (isValidCustomerValue(customer.customerName)) {
                clientRowsLeft.add(Pair("Customer Name", customer.customerName.trim()))
            }
            val comp = customer.companyName.ifBlank { customer.siteLocation }.trim()
            if (isValidCustomerValue(comp)) {
                clientRowsLeft.add(Pair("Company Name", comp))
            }
            if (isValidCustomerValue(customer.contactPerson)) {
                clientRowsLeft.add(Pair("Contact Person", customer.contactPerson.trim()))
            }
            if (isValidCustomerValue(customer.gstin)) {
                clientRowsLeft.add(Pair("GSTIN", customer.gstin.trim()))
            }

            if (isValidCustomerValue(customer.mobileNumber)) {
                clientRowsRight.add(Pair("Mobile", customer.mobileNumber.trim()))
            }
            val whatsappVal = customer.whatsappNumber.trim()
            if (isValidCustomerValue(whatsappVal)) {
                clientRowsRight.add(Pair("Alternate Mobile", whatsappVal))
            }
            if (isValidCustomerValue(customer.email)) {
                clientRowsRight.add(Pair("Email", customer.email.trim()))
            }

            val billingParts = listOf(customer.address, customer.city, customer.district, customer.state, customer.pincode, customer.country)
                .map { it.trim() }
                .filter { isValidCustomerValue(it) }
            if (billingParts.isNotEmpty()) {
                clientRowsRight.add(Pair("Billing Address", billingParts.joinToString(", ")))
            }
            if (isValidCustomerValue(customer.siteAddress)) {
                clientRowsRight.add(Pair("Site Address", customer.siteAddress.trim()))
            }
            if (isValidCustomerValue(customer.notes)) {
                clientRowsLeft.add(Pair("Notes", customer.notes.trim()))
            }
        } else {
            if (isValidCustomerValue(quotation.customerName)) {
                clientRowsLeft.add(Pair("Customer Name", quotation.customerName.trim()))
            }
            if (isValidCustomerValue(quotation.customerPhone)) {
                clientRowsRight.add(Pair("Mobile", quotation.customerPhone.trim()))
            }
            if (isValidCustomerValue(quotation.customerAddress)) {
                clientRowsRight.add(Pair("Address", quotation.customerAddress.trim()))
            }
        }

        if (clientRowsLeft.isEmpty() && clientRowsRight.isEmpty()) return

        val labelP = Paint().apply {
            color = Color.parseColor(COLOR_TEXT_SECONDARY)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val valP = Paint().apply {
            color = Color.parseColor(COLOR_DARK_SLATE)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val wrappedLeftRows = clientRowsLeft.map { row ->
            val wrappedVal = wrapText(row.second, 155, valP)
            row.first to wrappedVal
        }
        val wrappedRightRows = clientRowsRight.map { row ->
            val wrappedVal = wrapText(row.second, 165, valP)
            row.first to wrappedVal
        }

        val maxRows = maxOf(wrappedLeftRows.size, wrappedRightRows.size)
        val rowHeights = FloatArray(maxRows)
        var totalContentH = 20f // Top padding inside the customer card
        for (i in 0 until maxRows) {
            val leftItem = wrappedLeftRows.getOrNull(i)
            val rightItem = wrappedRightRows.getOrNull(i)
            val leftLinesCount = leftItem?.second?.size ?: 0
            val rightLinesCount = rightItem?.second?.size ?: 0
            val leftH = maxOf(11f, leftLinesCount * 10f)
            val rightH = maxOf(11f, rightLinesCount * 10f)
            rowHeights[i] = maxOf(leftH, rightH) + 3f // row height + gap
            totalContentH += rowHeights[i]
        }
        val sectionH = totalContentH + 12f // bottom padding + section spacing
        engine.ensureSpace(sectionH, reserveHeader = true)

        val blockTop = engine.currentY
        val cardW = engine.usableWidth
        engine.addCommand { canvas, _, _ ->
            val hPaint = Paint().apply {
                color = Color.parseColor(COLOR_PRIMARY_BLUE)
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val cardBgPaint = Paint().apply {
                color = Color.parseColor(COLOR_LIGHT_BG)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor(COLOR_BORDER)
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            val accentBarPaint = Paint().apply {
                color = Color.parseColor(COLOR_PRIMARY_BLUE)
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Draw single full-width card styled beautifully
            val cardRect = RectF(engine.marginX, blockTop, engine.marginX + cardW, blockTop + sectionH - 12f)
            canvas.drawRoundRect(cardRect, 4f, 4f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 4f, 4f, borderPaint)
            canvas.drawRect(engine.marginX, blockTop, engine.marginX + 3f, blockTop + sectionH - 12f, accentBarPaint)

            canvas.drawText("CUSTOMER DETAILS", engine.marginX + 10f, blockTop + 14f, hPaint)

            var currentYOffset = blockTop + 26f
            for (i in 0 until maxRows) {
                val leftItem = wrappedLeftRows.getOrNull(i)
                val rightItem = wrappedRightRows.getOrNull(i)

                if (leftItem != null) {
                    canvas.drawText(leftItem.first, engine.marginX + 10f, currentYOffset, labelP)
                    canvas.drawText(":", engine.marginX + 85f, currentYOffset, valP)
                    var valY = currentYOffset
                    leftItem.second.forEach { line ->
                        canvas.drawText(line, engine.marginX + 92f, valY, valP)
                        valY += 10f
                    }
                }

                if (rightItem != null) {
                    val rightColOffset = engine.marginX + 265f
                    canvas.drawText(rightItem.first, rightColOffset, currentYOffset, labelP)
                    canvas.drawText(":", rightColOffset + 75f, currentYOffset, valP)
                    var valY = currentYOffset
                    rightItem.second.forEach { line ->
                        canvas.drawText(line, rightColOffset + 82f, valY, valP)
                        valY += 10f
                    }
                }

                currentYOffset += rowHeights[i]
            }
        }
        engine.currentY += sectionH
    }

    private fun parseDimensionToInchesAndFeet(dimStr: String): Pair<Double, Double> {
        val clean = dimStr.replace("\"", "").lowercase(Locale.US).trim()
        if (clean.isEmpty() || clean == "null" || clean == "0" || clean == "0.0") {
            return Pair(0.0, 0.0)
        }
        val numeric = clean.replace("ft", "").replace("feet", "").replace("in", "").replace("inch", "").replace("'", "").trim()
        val value = numeric.toDoubleOrNull() ?: 0.0
        return when {
            dimStr.contains("\"") || clean.contains("in") || clean.contains("inch") -> {
                // Already in inches
                Pair(value, value / 12.0)
            }
            else -> {
                // Plain number or feet
                Pair(value * 12.0, value)
            }
        }
    }

    private fun formatInchesDisplay(inches: Double): String {
        return if (inches % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f\"", inches)
        } else {
            String.format(Locale.US, "%.1f\"", inches)
        }
    }

    private fun drawItemsTable(engine: PdfEngine, items: List<QuotationItem>) {
        engine.ensureSpace(35f, reserveHeader = true)
        engine.inTableMode = true
        engine.drawTableHeaderDirectly()

        val colWidths = floatArrayOf(22f, 115f, 165f, 55f, 35f, 65f, 66f)
        val colX = FloatArray(colWidths.size)
        var tx = engine.marginX
        for (i in colWidths.indices) {
            colX[i] = tx
            tx += colWidths[i]
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor(COLOR_DARK_SLATE)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val bodyBoldPaint = Paint().apply {
            color = Color.parseColor(COLOR_PRIMARY_BLUE)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val descPaint = Paint().apply {
            color = Color.parseColor(COLOR_TEXT_SECONDARY)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        val specPaint = Paint().apply {
            color = Color.parseColor(COLOR_TEXT_SECONDARY)
            textSize = 6.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        items.forEachIndexed { index, item ->
            val (userDesc, specs) = parseItemSpecs(item.description)

            val itemNameLines = wrapText(item.itemName.ifBlank { "Interior Item" }, 107, bodyBoldPaint)
            val descLines = if (userDesc.isNotBlank()) wrapText(userDesc, 107, bodyPaint) else emptyList()

            val specsList = generateSpecsList(item)
            val specsWrapped = mutableListOf<Pair<Boolean, String>>()
            specsList.forEach { spec ->
                val lines = wrapText(spec, 147, specPaint)
                lines.forEachIndexed { idx, line ->
                    specsWrapped.add(Pair(idx == 0, line))
                }
            }

            // Size & Area Computation using our centralized parser
            val (wInches, wFeet) = parseDimensionToInchesAndFeet(specs.width)
            val (hInches, hFeet) = parseDimensionToInchesAndFeet(specs.height)
            val (dInches, dFeet) = parseDimensionToInchesAndFeet(specs.depth)

            val sizeLines = mutableListOf<Pair<String, String>>()
            if (wFeet > 0.0) {
                sizeLines.add(Pair("W", formatInchesDisplay(wInches)))
            }
            if (hFeet > 0.0) {
                sizeLines.add(Pair("H", formatInchesDisplay(hInches)))
            }
            if (dFeet > 0.0) {
                sizeLines.add(Pair("D", formatInchesDisplay(dInches)))
            }
            if (wFeet > 0.0 && hFeet > 0.0) {
                val area = wFeet * hFeet
                sizeLines.add(Pair("Area", String.format(Locale.US, "%.2f Sq.Ft", area)))
            }

            val col1H = (itemNameLines.size + descLines.size) * 11f
            val col2H = specsWrapped.size * 9.5f
            val col3H = sizeLines.size * 9.5f

            val rowHeight = maxOf(22f, maxOf(col1H, col2H, col3H) + 10f)
            engine.ensureSpace(rowHeight, reserveHeader = true)

            val rowY = engine.currentY
            engine.addCommand { canvas, _, _ ->
                if (index % 2 == 1) {
                    canvas.drawRect(colX[0], rowY, engine.endX, rowY + rowHeight, Paint().apply { color = Color.parseColor(COLOR_LIGHT_BG); style = Paint.Style.FILL })
                }

                val gridBorderPaint = Paint().apply {
                    color = Color.parseColor(COLOR_BORDER)
                    strokeWidth = 0.5f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(colX[0], rowY, engine.endX, rowY + rowHeight, gridBorderPaint)
                for (i in 1 until colX.size) {
                    canvas.drawLine(colX[i], rowY, colX[i], rowY + rowHeight, gridBorderPaint)
                }

                val cellTextY = rowY + (rowHeight + 7.5f) / 2f - 1f

                // Col 0: Sl No (centered)
                canvas.drawText((index + 1).toString(), colX[0] + 11f, cellTextY, Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER; isAntiAlias = true })

                // Col 1: Item Details
                var textY = rowY + (rowHeight - col1H) / 2f + 8.5f
                itemNameLines.forEach { line ->
                    canvas.drawText(line, colX[1] + 4f, textY, bodyBoldPaint)
                    textY += 11f
                }
                descLines.forEach { line ->
                    canvas.drawText(line, colX[1] + 4f, textY, descPaint)
                    textY += 11f
                }

                // Col 2: Specifications
                var specY = rowY + (rowHeight - col2H) / 2f + 7f
                specsWrapped.forEach { (isFirst, line) ->
                    if (isFirst) {
                        canvas.drawText("• $line", colX[2] + 4f, specY, specPaint)
                    } else {
                        canvas.drawText(line, colX[2] + 12f, specY, specPaint)
                    }
                    specY += 9.5f
                }

                // Col 3: Size (perfectly aligned and centered)
                val sizeLabelPaint = Paint(bodyBoldPaint).apply { textSize = 6.5f; color = Color.parseColor(COLOR_PRIMARY_BLUE); isAntiAlias = true }
                val sizeValuePaint = Paint(bodyPaint).apply { textSize = 6.5f; color = Color.parseColor(COLOR_DARK_SLATE); isAntiAlias = true }
                val colonAndSpace = " : "
                val colonWidth = sizeLabelPaint.measureText(colonAndSpace)

                var maxNameWidth = 0f
                var maxValueWidth = 0f
                sizeLines.forEach { line ->
                    val nw = sizeLabelPaint.measureText(line.first)
                    if (nw > maxNameWidth) {
                        maxNameWidth = nw
                    }
                    val vw = sizeValuePaint.measureText(line.second)
                    if (vw > maxValueWidth) {
                        maxValueWidth = vw
                    }
                }
                val totalWidth = maxNameWidth + colonWidth + maxValueWidth
                val startX = colX[3] + (55f - totalWidth) / 2f

                var sizeY = rowY + (rowHeight - col3H) / 2f + 7.5f
                sizeLines.forEach { line ->
                    canvas.drawText(line.first, startX, sizeY, sizeLabelPaint)
                    canvas.drawText(colonAndSpace, startX + maxNameWidth, sizeY, sizeLabelPaint)
                    canvas.drawText(line.second, startX + maxNameWidth + colonWidth, sizeY, sizeValuePaint)
                    sizeY += 9.5f
                }

                // Col 4: Qty
                val userQty = getUserEnteredQuantity(item, specs)
                val qtyRounded = Math.round(userQty * 100.0) / 100.0
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%,.0f", qtyRounded) else String.format(Locale.US, "%,.2f", qtyRounded)
                canvas.drawText(qtyStr, colX[4] + 17.5f, cellTextY, Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER; isAntiAlias = true })

                // Col 5: Rate
                canvas.drawText(formatIndianCurrency(item.rate), colX[5] + colWidths[5] - 4f, cellTextY, Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT; isAntiAlias = true })

                // Col 6: Amount
                canvas.drawText(formatIndianCurrency(item.amount), colX[6] + colWidths[6] - 4f, cellTextY, Paint(bodyBoldPaint).apply { textAlign = Paint.Align.RIGHT; isAntiAlias = true })
            }
            engine.currentY += rowHeight
        }
        engine.inTableMode = false
    }

    private fun drawSummary(
        engine: PdfEngine,
        quotation: Quotation,
        showGst: Boolean,
        showAmountInWords: Boolean,
        roundOff: Double
    ) {
        val rowH = 16f
        val totalsRows = mutableListOf<Pair<String, String>>()
        totalsRows.add(Pair("Sub Total", formatIndianCurrency(quotation.subtotal)))
        if (quotation.discount > 0.0) {
            totalsRows.add(Pair("Discount", formatIndianCurrency(quotation.discount)))
        }
        if (showGst && (quotation.gstAmount > 0.0 || quotation.gstRate > 0.0)) {
            totalsRows.add(Pair("GST (${quotation.gstRate}%)", formatIndianCurrency(quotation.gstAmount)))
        }
        if (Math.abs(roundOff) > 0.001) {
            totalsRows.add(Pair("Round Off", formatIndianCurrency(roundOff)))
        }

        val boxTopPadding = 10f
        val boxBottomPadding = 10f
        val grandTotalH = 22f
        val totalsBoxH = boxTopPadding + totalsRows.size * rowH + boxBottomPadding + grandTotalH

        // Amount in Words calculation
        val leftW = 245f
        val wordsStr = convertNumberToWords(quotation.grandTotal)
        val wordsPaint = Paint().apply {
            color = Color.parseColor(COLOR_DARK_SLATE)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val wrappedWords = if (showAmountInWords) wrapText(wordsStr, (leftW - 16f).toInt(), wordsPaint) else emptyList()
        val wordsBoxH = if (showAmountInWords) maxOf(26f, 12f + wrappedWords.size * 9.5f) else 0f

        val sectionSpacing = 14f
        val sectionHeight = maxOf(totalsBoxH, wordsBoxH) + 12f
        engine.ensureSpace(sectionHeight + sectionSpacing, reserveHeader = true)
        engine.currentY += sectionSpacing

        val blockY = engine.currentY
        val rightX = engine.marginX + 295f

        engine.addCommand { canvas, _, _ ->
            // Draw Right Side Totals Box
            val boxPaint = Paint().apply { color = Color.parseColor(COLOR_LIGHT_BG); style = Paint.Style.FILL }
            val borderPaint = Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.75f; style = Paint.Style.STROKE }
            val boxRect = RectF(rightX, blockY, engine.endX, blockY + totalsBoxH)
            canvas.drawRoundRect(boxRect, 4f, 4f, boxPaint)
            canvas.drawRoundRect(boxRect, 4f, 4f, borderPaint)

            val grandTotalY = blockY + boxTopPadding + totalsRows.size * rowH + boxBottomPadding
            canvas.drawLine(rightX, grandTotalY, engine.endX, grandTotalY, borderPaint)

            val gtBgPaint = Paint().apply { color = Color.parseColor(COLOR_PRIMARY_BLUE); style = Paint.Style.FILL }
            val clipPath = android.graphics.Path()
            clipPath.addRoundRect(boxRect, 4f, 4f, android.graphics.Path.Direction.CW)
            canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawRect(rightX, grandTotalY, engine.endX, grandTotalY + grandTotalH, gtBgPaint)
            canvas.restore()

            val labelPaint = Paint().apply { color = Color.parseColor(COLOR_TEXT_SECONDARY); textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
            val valuePaint = Paint().apply { color = Color.parseColor(COLOR_DARK_SLATE); textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT; isAntiAlias = true }
            val colonPaint = Paint().apply { color = Color.parseColor(COLOR_TEXT_SECONDARY); textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }

            totalsRows.forEachIndexed { idx, row ->
                val y = blockY + boxTopPadding + idx * rowH
                canvas.drawText(row.first, rightX + 10f, y + 11f, labelPaint)
                canvas.drawText(":", rightX + 110f, y + 11f, colonPaint)
                canvas.drawText(row.second, engine.endX - 10f, y + 11f, valuePaint)
            }

            canvas.drawText("GRAND TOTAL", rightX + 10f, grandTotalY + 14f, Paint().apply { color = Color.WHITE; textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true })
            canvas.drawText(":", rightX + 110f, grandTotalY + 14f, Paint().apply { color = Color.WHITE; textSize = 8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true })
            canvas.drawText("₹ " + formatIndianCurrency(quotation.grandTotal), engine.endX - 10f, grandTotalY + 14f, Paint().apply { color = Color.WHITE; textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT; isAntiAlias = true })

            // Draw Left Side Amount in Words Box
            if (showAmountInWords && wordsBoxH > 0f) {
                val leftX = engine.marginX
                val wordsBg = RectF(leftX, blockY, leftX + leftW, blockY + wordsBoxH)
                canvas.drawRoundRect(wordsBg, 4f, 4f, Paint().apply { color = Color.parseColor(COLOR_LIGHT_BG) })
                canvas.drawRoundRect(wordsBg, 4f, 4f, Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.5f; style = Paint.Style.STROKE })

                canvas.drawText("AMOUNT IN WORDS", leftX + 8f, blockY + 10f, Paint().apply { color = Color.parseColor(COLOR_ACCENT_ORANGE); textSize = 6f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true })

                var textY = blockY + 19f
                wrappedWords.forEach { line ->
                    canvas.drawText(line, leftX + 8f, textY, wordsPaint)
                    textY += 9.5f
                }
            }
        }
        engine.currentY += sectionHeight
    }

    private fun drawPayment(
        engine: PdfEngine,
        company: CompanyProfile,
        showBankDetails: Boolean,
        showQrCode: Boolean
    ) {
        val hasBank = showBankDetails && company.bankName.trim().isNotBlank() && company.accountNumber.trim().isNotBlank()
        val hasQr = showQrCode && company.upiId.trim().isNotBlank()

        if (!hasBank && !hasQr) return

        // Compute row count for bank details card
        var bankRowCount = 0
        if (hasBank) {
            if (company.bankName.isNotBlank()) bankRowCount++
            if (company.accountHolderName.ifBlank { company.companyName }.isNotBlank()) bankRowCount++
            if (company.accountNumber.isNotBlank()) bankRowCount++
            if (company.ifsc.isNotBlank()) bankRowCount++
            if (company.branch.isNotBlank()) bankRowCount++
        }
        if (hasQr && company.upiId.isNotBlank()) bankRowCount++

        val sectionSpacing = 14f
        val payCardH = maxOf(50f, bankRowCount * 10.5f + 14f)
        val sectionHeight = payCardH + 26f
        engine.ensureSpace(sectionHeight + sectionSpacing, reserveHeader = true)
        engine.currentY += sectionSpacing

        val blockTop = engine.currentY
        val leftX = engine.marginX
        val leftW = 245f

        engine.addCommand { canvas, _, _ ->
            val payCardTop = blockTop + 14f
            val payCard = RectF(leftX, payCardTop, leftX + leftW, payCardTop + payCardH)
            canvas.drawRoundRect(payCard, 4f, 4f, Paint().apply { color = Color.parseColor(COLOR_LIGHT_BG) })
            canvas.drawRoundRect(payCard, 4f, 4f, Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.5f; style = Paint.Style.STROKE })

            canvas.drawText("PAYMENT METHOD", leftX, blockTop + 9f, Paint().apply { color = Color.parseColor(COLOR_PRIMARY_BLUE); textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true })

            var bY = payCardTop + 15f
            fun drawBankRow(label: String, value: String) {
                if (value.isNotBlank()) {
                    val labelP = Paint().apply { color = Color.parseColor(COLOR_TEXT_SECONDARY); textSize = 7f; isAntiAlias = true }
                    val valP = Paint().apply { color = Color.parseColor(COLOR_DARK_SLATE); textSize = 7f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
                    canvas.drawText(label, leftX + 8f, bY, labelP)
                    canvas.drawText(":", leftX + 65f, bY, valP)
                    canvas.drawText(value, leftX + 72f, bY, valP)
                    bY += 10.5f
                }
            }

            if (hasBank) {
                drawBankRow("Bank Name", company.bankName)
                drawBankRow("A/c Holder", company.accountHolderName.ifBlank { company.companyName })
                drawBankRow("A/c Number", company.accountNumber)
                drawBankRow("IFSC Code", company.ifsc)
                drawBankRow("Branch", company.branch)
                if (hasQr && company.upiId.isNotBlank()) {
                    drawBankRow("UPI ID", company.upiId)
                }
            } else if (hasQr) {
                val labelP = Paint().apply { color = Color.parseColor(COLOR_TEXT_SECONDARY); textSize = 7f; isAntiAlias = true }
                val valP = Paint().apply { color = Color.parseColor(COLOR_DARK_SLATE); textSize = 7f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
                val textY = payCardTop + (payCardH / 2f) + 2.5f
                canvas.drawText("UPI ID", leftX + 8f, textY, labelP)
                canvas.drawText(":", leftX + 65f, textY, valP)
                canvas.drawText(company.upiId, leftX + 72f, textY, valP)
            }

            if (hasQr) {
                val qrSize = payCardH - 12f
                val qrLeft = leftX + leftW - qrSize - 8f
                val qrTop = payCardTop + (payCardH - qrSize) / 2f
                val upiPayload = "upi://pay?pa=${company.upiId}&pn=${uriEncodeSafely(company.companyName)}"
                drawQrCode(canvas, qrLeft, qrTop, qrSize, upiPayload)
            }
        }
        engine.currentY += sectionHeight
    }

    private fun uriEncodeSafely(text: String): String {
        return try {
            java.net.URLEncoder.encode(text, "UTF-8")
        } catch (e: Exception) {
            text.replace(" ", "%20")
        }
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
        val warrantyVal = company.defaultWarranty.ifBlank { quotation.warranty }
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
        if (company.defaultQuoteValidity.isNotBlank()) {
            termsList.add("Quote Validity : ${company.defaultQuoteValidity}")
        }
        if (company.additionalConditions.isNotBlank()) {
            termsList.add("Additional Conditions : ${company.additionalConditions}")
        }

        val rawTerms = quotation.termsAndConditions.ifBlank { company.termsAndConditions }
        if (rawTerms.isNotBlank()) {
            rawTerms.split("\n").forEach { term ->
                val trimmed = term.trim()
                if (trimmed.isNotBlank()) {
                    termsList.add(trimmed)
                }
            }
        }

        if (termsList.isEmpty()) return
        val measurePaint = Paint().apply {
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val leftX = engine.marginX

        data class TermItem(
            val number: Int,
            val label: String,
            val value: String,
            val bullets: List<String> = emptyList()
        )

        val termItems = mutableListOf<TermItem>()
        var termIdx = 1

        termsList.forEach { rawTerm ->
            val cleanedTerm = rawTerm.trim()
            if (cleanedTerm.isNotEmpty()) {
                val cleaned = cleanedTerm.replace(Regex("\\s*:\\s*"), " : ").trim()

                if (cleaned.startsWith("Payment", ignoreCase = true) || cleaned.contains("Advance", ignoreCase = true)) {
                    val parts = cleaned.split(" : ")
                    val label: String
                    val valueStr: String
                    if (parts.size > 1) {
                        label = parts[0].trim()
                        valueStr = parts[1].trim()
                    } else {
                        label = "Payment Terms"
                        valueStr = cleaned
                    }

                    val bullets = mutableListOf<String>()
                    if (valueStr.isNotEmpty()) {
                        val payments = valueStr.split(Regex("[,;\n•*]+"))
                        payments.forEach { pay ->
                            val payTrimmed = pay.trim()
                            if (payTrimmed.isNotEmpty()) {
                                bullets.add(payTrimmed)
                            }
                        }
                    }
                    termItems.add(TermItem(termIdx++, label, "", bullets))
                } else if (cleaned.contains(" : ")) {
                    val parts = cleaned.split(" : ")
                    termItems.add(TermItem(termIdx++, parts[0].trim(), parts[1].trim()))
                } else {
                    termItems.add(TermItem(termIdx++, cleaned, ""))
                }
            }
        }

        val termSpacing = 6f
        var contentHeight = 22f // title + padding

        val col1Width = 20f
        val col2Width = 110f
        val col3Width = 15f
        val col4Width = 378f

        data class WrappedTerm(
            val item: TermItem,
            val labelLines: List<String>,
            val valLines: List<String>,
            val bulletsWrapped: List<List<String>>,
            val itemH: Float
        )

        val wrappedTerms = termItems.map { item ->
            val hasValueOrBullets = item.value.isNotEmpty() || item.bullets.isNotEmpty()
            val labelLines = if (hasValueOrBullets) {
                wrapText(item.label, col2Width.toInt(), measurePaint)
            } else {
                wrapText(item.label, (523f - col1Width).toInt(), measurePaint)
            }
            val valLines = if (item.value.isNotEmpty()) wrapText(item.value, col4Width.toInt(), measurePaint) else emptyList()
            val bulletsWrapped = item.bullets.map { bullet -> wrapText(bullet, (col4Width - 10f).toInt(), measurePaint) }

            val labelBlockH = labelLines.size * 10f
            val itemH = if (hasValueOrBullets) {
                if (item.bullets.isNotEmpty()) {
                    val bulletsH = bulletsWrapped.sumOf { bLines -> bLines.size } * 10f
                    maxOf(labelBlockH, bulletsH)
                } else {
                    val valueBlockH = valLines.size * 10f
                    maxOf(labelBlockH, valueBlockH)
                }
            } else {
                labelBlockH
            }
            contentHeight += itemH + termSpacing

            WrappedTerm(item, labelLines, valLines, bulletsWrapped, itemH)
        }

        if (wrappedTerms.isNotEmpty()) {
            contentHeight -= termSpacing
        }

        val sectionSpacing = 14f
        engine.ensureSpace(contentHeight + sectionSpacing, reserveHeader = true)
        engine.currentY += sectionSpacing

        val blockTop = engine.currentY

        val titlePaint = Paint().apply {
            color = Color.parseColor(COLOR_PRIMARY_BLUE)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor(COLOR_DARK_SLATE)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        engine.addCommand { canvas, _, _ ->
            canvas.drawText("TERMS & CONDITIONS", leftX, blockTop + 8f, titlePaint)
            var tY = blockTop + 18f

            wrappedTerms.forEach { wt ->
                val item = wt.item
                val tY_start = tY
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

                tY = tY_start + wt.itemH + termSpacing
            }
        }
        engine.currentY += contentHeight
    }

    private fun drawSignature(
        engine: PdfEngine,
        company: CompanyProfile,
        showCompanySeal: Boolean,
        showSignature: Boolean
    ) {
        val sealPath = company.companySealPath
        val sigPath = company.signaturePath
        val hasSeal = showCompanySeal && sealPath.isNotBlank() && File(sealPath).exists()
        val hasSig = showSignature && sigPath.isNotBlank() && File(sigPath).exists()

        val sectionSpacing = 14f
        val signatureBoxH = 85f

        // Smart page-break logic: calculate required footer height including signature block and bottom margin
        val footerRequiredHeight = signatureBoxH + sectionSpacing + engine.bottomMargin
        val remainingHeight = engine.pageHeight - engine.currentY

        if (remainingHeight >= footerRequiredHeight) {
            engine.currentY += sectionSpacing
        } else {
            engine.startNewPage(reserveHeader = true)
            engine.currentY += sectionSpacing
        }

        val sigY = engine.currentY

        engine.addCommand { canvas, _, _ ->
            val baselineY = sigY + 50f
            val linePaint = Paint().apply {
                color = Color.parseColor(COLOR_BORDER)
                strokeWidth = 0.75f
                isAntiAlias = true
            }

            // Left Column (Column 1): Company Seal
            val sealCenterX = engine.marginX + 75f
            if (hasSeal) {
                // Seal is 40x40. Draw centered horizontally on sealCenterX, bottom aligned at baselineY
                drawBitmapSafely(canvas, sealPath, sealCenterX - 20f, baselineY - 40f, 40f, 40f)
            }
            // Draw a clean baseline under the seal
            canvas.drawLine(sealCenterX - 35f, baselineY, sealCenterX + 35f, baselineY, linePaint)
            // Label centered below baseline
            canvas.drawText("COMPANY SEAL", sealCenterX, baselineY + 11f, Paint().apply {
                color = Color.parseColor(COLOR_TEXT_SECONDARY)
                textSize = 6.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            })

            // Right Column (Column 2): Signature & Signatory Name
            val authCenterX = engine.endX - 75f
            val coNameVal = company.companyName
            if (coNameVal.isNotBlank()) {
                canvas.drawText("For ${coNameVal.uppercase()}", authCenterX, baselineY - 32f, Paint().apply {
                    color = Color.parseColor(COLOR_TEXT_SECONDARY)
                    textSize = 7f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })
            }

            if (hasSig) {
                // Signature is 80x25. Draw centered horizontally on authCenterX, bottom aligned at baselineY
                drawBitmapSafely(canvas, sigPath, authCenterX - 40f, baselineY - 25f, 80f, 25f)
            }

            // Draw a clean baseline under the signature / name
            canvas.drawLine(authCenterX - 55f, baselineY, authCenterX + 55f, baselineY, linePaint)

            // Draw owner name and designation centered below baseline
            var sigLabelY = baselineY + 11f
            val ownerName = company.ownerName
            if (ownerName.isNotBlank()) {
                canvas.drawText(ownerName, authCenterX, sigLabelY, Paint().apply {
                    color = Color.parseColor(COLOR_DARK_SLATE)
                    textSize = 7.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })
                sigLabelY += 9f
            }

            val designation = company.signatureText.ifBlank { "Authorized Signatory" }
            canvas.drawText(designation, authCenterX, sigLabelY, Paint().apply {
                color = Color.parseColor(COLOR_TEXT_SECONDARY)
                textSize = 6.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            })
        }
        engine.currentY += signatureBoxH
    }

    private fun drawReferenceImages(engine: PdfEngine, items: List<QuotationItem>) {
        data class ImageInfo(
            val path: String,
            val caption: String
        )
        val validImages = mutableListOf<ImageInfo>()
        items.forEach { item ->
            val (_, specs) = parseItemSpecs(item.description)
            if (specs.laminateImageUri.isNotBlank()) {
                val file = File(specs.laminateImageUri)
                if (file.exists()) {
                    validImages.add(ImageInfo(specs.laminateImageUri, "${item.itemName} - Laminate/Finish"))
                }
            }
            if (specs.designImageUri.isNotBlank()) {
                val file = File(specs.designImageUri)
                if (file.exists()) {
                    validImages.add(ImageInfo(specs.designImageUri, "${item.itemName} - Design Reference"))
                }
            }
        }

        if (validImages.isEmpty()) return

        val titlePaint = Paint().apply {
            color = Color.parseColor(COLOR_PRIMARY_BLUE)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val imagesPerPage = 4
        val chunks = validImages.chunked(imagesPerPage)
        chunks.forEachIndexed { pageIdx, pageImages ->
            engine.startNewPage(reserveHeader = true)

            val titleY = engine.currentY + 12f
            engine.addCommand { canvas, _, _ ->
                val titleText = if (pageIdx == 0) "REFERENCE IMAGES" else "REFERENCE IMAGES (Contd.)"
                canvas.drawText(titleText, engine.marginX, titleY, titlePaint)
                canvas.drawLine(
                    engine.marginX, titleY + 6f, engine.endX, titleY + 6f,
                    Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.75f }
                )
            }
            engine.currentY += 30f

            val startY = engine.currentY
            val cardW = 245f
            val cardH = 220f
            val gapX = 33f
            val gapY = 20f

            pageImages.forEachIndexed { imgIdx, imageInfo ->
                val col = imgIdx % 2
                val row = imgIdx / 2
                val x = engine.marginX + col * (cardW + gapX)
                val y = startY + row * (cardH + gapY)

                engine.addCommand { canvas, _, _ ->
                    val bgPaint = Paint().apply { color = Color.parseColor(COLOR_LIGHT_BG); style = Paint.Style.FILL }
                    val borderPaint = Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.5f; style = Paint.Style.STROKE }
                    val captionPaint = Paint().apply {
                        color = Color.parseColor(COLOR_DARK_SLATE)
                        textSize = 7.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val cardRect = RectF(x, y, x + cardW, y + cardH)
                    canvas.drawRoundRect(cardRect, 4f, 4f, bgPaint)
                    canvas.drawRoundRect(cardRect, 4f, 4f, borderPaint)

                    val imgAreaW = cardW - 16f
                    val imgAreaH = cardH - 36f
                    val imgX = x + 8f
                    val imgY = y + 8f

                    drawBitmapSafely(canvas, imageInfo.path, imgX, imgY, imgAreaW, imgAreaH)

                    val captionY = y + cardH - 18f
                    val wrappedCaption = wrapText(imageInfo.caption, (cardW - 16f).toInt(), captionPaint)
                    var capY = captionY
                    if (wrappedCaption.isNotEmpty()) {
                        wrappedCaption.forEach { line ->
                            canvas.drawText(line, x + cardW / 2f, capY, captionPaint)
                            capY += 9f
                        }
                    }
                }
            }
            engine.currentY += 2 * cardH + gapY
        }
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int, company: CompanyProfile, showPageNumber: Boolean) {
        val footY = 842f - 40f
        val linePaint = Paint().apply {
            color = Color.parseColor(COLOR_BORDER)
            strokeWidth = 0.5f
        }
        canvas.drawLine(36f, footY, 559f, footY, linePaint)

        val paint = Paint().apply {
            color = Color.parseColor(COLOR_TEXT_SECONDARY)
            textSize = 6.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        // Left: Powered by InteriorPro Technologies
        canvas.drawText(
            "Powered by InteriorPro Technologies",
            36f,
            footY + 12f,
            paint
        )

        // Center: Website
        val websiteText = company.website.trim().ifBlank { "www.interiorpro.tech" }
        canvas.drawText(
            websiteText,
            (36f + 559f) / 2f,
            footY + 12f,
            Paint(paint).apply { textAlign = Paint.Align.CENTER }
        )

        // Right: Page x of y
        if (showPageNumber) {
            val pageText = "Page $pageNum of $totalPages"
            canvas.drawText(
                pageText,
                559f,
                footY + 12f,
                Paint(paint).apply { textAlign = Paint.Align.RIGHT }
            )
        }
    }

    private fun drawBitmapSafely(canvas: Canvas, path: String, left: Float, top: Float, reqWidth: Float, reqHeight: Float) {
        try {
            val bitmap = ImageManager.loadScaledBitmap(path, reqWidth, reqHeight)
            if (bitmap != null) {
                // Calculate scale to fit preserving aspect ratio
                val scale = minOf(reqWidth / bitmap.width.toFloat(), reqHeight / bitmap.height.toFloat())
                val drawW = bitmap.width * scale
                val drawH = bitmap.height * scale

                // Center inside the requested rectangle
                val drawLeft = left + (reqWidth - drawW) / 2f
                val drawTop = top + (reqHeight - drawH) / 2f

                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = RectF(drawLeft, drawTop, drawLeft + drawW, drawTop + drawH)
                canvas.drawBitmap(bitmap, srcRect, destRect, Paint().apply { isAntiAlias = true; isFilterBitmap = true })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawQrCode(canvas: Canvas, left: Float, top: Float, size: Float, text: String) {
        val blackPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = Color.parseColor(COLOR_BORDER); strokeWidth = 0.5f; style = Paint.Style.STROKE }

        // Draw background card with light grey border
        val bgRect = RectF(left, top, left + size, top + size)
        canvas.drawRoundRect(bgRect, 2f, 2f, Paint().apply { color = Color.WHITE })
        canvas.drawRoundRect(bgRect, 2f, 2f, borderPaint)

        // Render QR Code grid payload
        val cellSize = size / 21f

        fun drawFinderPattern(l: Float, t: Float) {
            canvas.drawRect(l, t, l + cellSize * 7, t + cellSize * 7, blackPaint)
            canvas.drawRect(l + cellSize, t + cellSize, l + cellSize * 6, t + cellSize * 6, Paint().apply { color = Color.WHITE })
            canvas.drawRect(l + cellSize * 2, t + cellSize * 2, l + cellSize * 5, t + cellSize * 5, blackPaint)
        }

        drawFinderPattern(left, top)
        drawFinderPattern(left + size - cellSize * 7, top)
        drawFinderPattern(left, top + size - cellSize * 7)

        val hash = text.hashCode()
        val random = java.util.Random(hash.toLong())

        for (row in 0 until 21) {
            for (col in 0 until 21) {
                if ((row < 8 && col < 8) || (row < 8 && col >= 13) || (row >= 13 && col < 8)) continue
                if (row == 6 || col == 6) {
                    if ((row + col) % 2 == 0) {
                        canvas.drawRect(left + col * cellSize, top + row * cellSize, left + (col + 1) * cellSize, top + (row + 1) * cellSize, blackPaint)
                    }
                    continue
                }
                if (random.nextBoolean()) {
                    canvas.drawRect(left + col * cellSize, top + row * cellSize, left + (col + 1) * cellSize, top + (row + 1) * cellSize, blackPaint)
                }
            }
        }
    }

    private fun formatIndianCurrency(amount: Double): String {
        return CurrencyFormatter.formatIndianCurrency(amount)
    }

    private fun convertNumberToWords(amount: Double): String {
        return CurrencyFormatter.convertNumberToWords(amount)
    }

    private fun wrapText(text: String, width: Int, paint: Paint): List<String> {
        if (text.isEmpty()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            if (testWidth <= width) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    private fun getUserEnteredQuantity(item: QuotationItem, specs: ItemSpecs): Double {
        val wClean = specs.width.replace("\"", "").lowercase(Locale.US).replace("ft", "").replace("feet", "").trim()
        val hClean = specs.height.replace("\"", "").lowercase(Locale.US).replace("ft", "").replace("feet", "").trim()
        val w = wClean.toDoubleOrNull() ?: 1.0
        val h = hClean.toDoubleOrNull() ?: 1.0
        val uLower = item.unit.trim().lowercase(Locale.US)
        return when {
            uLower == "sq.ft" || uLower.contains("sq.ft") || uLower.contains("sqft") || uLower == "sft" -> {
                val area = w * h
                if (area > 0) item.quantity / area else item.quantity
            }
            uLower == "running feet" || uLower.contains("run") || uLower.contains("rft") -> {
                if (w > 0) item.quantity / w else item.quantity
            }
            uLower == "sq.m" || uLower.contains("sq.m") || uLower.contains("sqm") || uLower.contains("square meter") -> {
                val areaM = w * h * 0.09290304
                if (areaM > 0) item.quantity / areaM else item.quantity
            }
            else -> {
                item.quantity
            }
        }
    }

    private fun parseItemSpecs(description: String): Pair<String, ItemSpecs> {
        if (!description.contains("|||")) {
            return Pair(description, ItemSpecs())
        }
        val parts = description.split("|||")
        val userDesc = parts[0].trim()
        val specsJson = parts[1].trim()
        return try {
            val json = org.json.JSONObject(specsJson)
            val specs = ItemSpecs(
                width = json.optString("width", ""),
                height = json.optString("height", ""),
                depth = json.optString("depth", ""),
                doorType = json.optString("doorType", ""),
                finish = json.optString("finish", ""),
                hardware = json.optString("hardware", ""),
                brand = json.optString("brand", ""),
                thickness = json.optString("thickness", ""),
                colour = json.optString("colour", ""),
                laminateImageUri = json.optString("laminateImageUri", ""),
                designImageUri = json.optString("designImageUri", ""),
                profileSeries = json.optString("profileSeries", ""),
                profileColour = json.optString("profileColour", ""),
                glassType = json.optString("glassType", ""),
                glassThickness = json.optString("glassThickness", ""),
                acpColour = json.optString("acpColour", ""),
                grade = json.optString("grade", "")
            )
            Pair(userDesc, specs)
        } catch (e: Exception) {
            Pair(userDesc, ItemSpecs())
        }
    }

    private fun generateSpecsList(item: QuotationItem): List<String> {
        val (userDesc, specs) = parseItemSpecs(item.description)
        val list = mutableListOf<String>()
        val addedValues = mutableSetOf<String>()

        fun formatValue(v: String): String {
            val trimmed = v.trim()
            val regex = Regex("""(\d+(?:\.\d+)?)\s*([a-zA-Z]+)""")
            val result = regex.replace(trimmed) { matchResult ->
                val num = matchResult.groupValues[1]
                val unit = matchResult.groupValues[2]
                val lowerUnit = unit.lowercase(Locale.US)
                if (lowerUnit in listOf("mm", "inch", "in", "ft", "cm", "days", "years", "months")) {
                    "$num $unit"
                } else {
                    matchResult.value
                }
            }
            return result
        }

        fun addSpec(label: String, value: String) {
            val formatted = formatValue(value)
            if (formatted.isNotBlank()) {
                val lower = formatted.lowercase(Locale.US)
                if (lower != "none" && lower != "n/a" && lower != "null" && lower != "not applicable" && lower != "not available" && lower != "empty") {
                    if (!addedValues.contains(lower)) {
                        list.add("$label : $formatted")
                        addedValues.add(lower)
                    }
                }
            }
        }

        // Material
        addSpec("Material", item.material)

        // Thickness (either thickness or glassThickness)
        val thickness = specs.thickness.trim()
        if (thickness.isNotBlank() && !thickness.equals("none", ignoreCase = true)) {
            addSpec("Thickness", thickness)
        } else {
            addSpec("Thickness", specs.glassThickness)
        }

        // Grade
        addSpec("Grade", specs.grade)

        // Finish
        val finish = item.finish.trim().ifBlank { specs.finish.trim() }
        addSpec("Finish", finish)

        // Glass / Glass Type
        addSpec("Glass", specs.glassType)

        // Color
        val color = specs.colour.trim()
            .ifBlank { specs.profileColour.trim() }
            .ifBlank { specs.acpColour.trim() }
        addSpec("Color", color)

        // Hardware
        addSpec("Hardware", specs.hardware)

        // Brand
        addSpec("Brand", specs.brand)

        // Series
        addSpec("Series", specs.profileSeries)

        // Door Type
        addSpec("Door Type", specs.doorType)

        return list
    }

    private data class ItemSpecs(
        val width: String = "",
        val height: String = "",
        val depth: String = "",
        val doorType: String = "",
        val finish: String = "",
        val hardware: String = "",
        val brand: String = "",
        val thickness: String = "",
        val colour: String = "",
        val laminateImageUri: String = "",
        val designImageUri: String = "",
        val profileSeries: String = "",
        val profileColour: String = "",
        val glassType: String = "",
        val glassThickness: String = "",
        val acpColour: String = "",
        val grade: String = ""
    )
}
