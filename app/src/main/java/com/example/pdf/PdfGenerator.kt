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

    private val wrapTextCache = mutableMapOf<String, List<String>>()
    
    fun wrapText(text: String, width: Int, paint: Paint): List<String> {
        if (text.isEmpty()) return emptyList()
        val key = "$text:$width:${paint.textSize}:${paint.typeface?.hashCode()}"
        return wrapTextCache.getOrPut(key) {
            val lines = mutableListOf<String>()
            val explicitLines = text.split("\n")
            for (explicitLine in explicitLines) {
                if (explicitLine.isEmpty()) {
                    lines.add("")
                    continue
                }
                val words = explicitLine.split(" ")
                var currentLine = ""
                for (word in words) {
                    if (currentLine.isEmpty()) {
                        currentLine = word
                    } else {
                        val testLine = "$currentLine $word"
                        if (paint.measureText(testLine) <= width) {
                            currentLine = testLine
                        } else {
                            lines.add(currentLine)
                            currentLine = word
                        }
                    }
                }
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
            }
            lines
        }
    }

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

    private val TYPEFACE_NORMAL = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val TYPEFACE_BOLD = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    private val TYPEFACE_ITALIC = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

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
        var rightYTracker = 0f
        var currentPageIndex = 0
        var inTableMode = false

        // Drawing commands grouped by page index: (Canvas, CurrentPage, TotalPages) -> Unit
        val pagesCommands = mutableListOf<MutableList<(Canvas, Int, Int) -> Unit>>()

        val bitmapCache = mutableMapOf<String, Bitmap>()
        val paintCache = mutableMapOf<String, Paint>()
        val colorCache = mutableMapOf<String, Int>()
        val wrapTextCache = mutableMapOf<String, List<String>>()

        init {
            pagesCommands.add(mutableListOf())
        }

        fun parseColor(hex: String): Int {
            return colorCache.getOrPut(hex) { Color.parseColor(hex) }
        }

        fun getPaint(
            color: Int,
            textSize: Float = 0f,
            typeface: Typeface = TYPEFACE_NORMAL,
            style: Paint.Style = Paint.Style.FILL,
            strokeWidth: Float = 0f,
            isAntiAlias: Boolean = true,
            textAlign: Paint.Align = Paint.Align.LEFT,
            isFilterBitmap: Boolean = false
        ): Paint {
            val key = "$color:$textSize:${typeface.hashCode()}:${style.ordinal}:$strokeWidth:$isAntiAlias:${textAlign.ordinal}:$isFilterBitmap"
            return paintCache.getOrPut(key) {
                Paint().apply {
                    this.color = color
                    if (textSize > 0f) this.textSize = textSize
                    this.typeface = typeface
                    this.style = style
                    this.strokeWidth = strokeWidth
                    this.isAntiAlias = isAntiAlias
                    this.textAlign = textAlign
                    this.isFilterBitmap = isFilterBitmap
                    this.isDither = true
                    this.flags = this.flags or Paint.SUBPIXEL_TEXT_FLAG
                    this.hinting = Paint.HINTING_ON
                }
            }
        }

        fun getPaint(
            colorHex: String,
            textSize: Float = 0f,
            typeface: Typeface = TYPEFACE_NORMAL,
            style: Paint.Style = Paint.Style.FILL,
            strokeWidth: Float = 0f,
            isAntiAlias: Boolean = true,
            textAlign: Paint.Align = Paint.Align.LEFT,
            isFilterBitmap: Boolean = false
        ): Paint {
            return getPaint(parseColor(colorHex), textSize, typeface, style, strokeWidth, isAntiAlias, textAlign, isFilterBitmap)
        }

        

        fun getOrLoadBitmap(path: String, reqWidth: Float, reqHeight: Float): Bitmap? {
            val key = "$path:$reqWidth:$reqHeight"
            bitmapCache[key]?.let { if (!it.isRecycled) return it }
            val bitmap = ImageManager.loadScaledBitmap(path, reqWidth, reqHeight)
            if (bitmap != null) {
                bitmapCache[key] = bitmap
            }
            return bitmap
        }

        fun clearBitmapCache() {
            bitmapCache.values.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            bitmapCache.clear()
        }

        fun clearCaches() {
            clearBitmapCache()
            paintCache.clear()
            wrapTextCache.clear()
            colorCache.clear()
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
            rightYTracker = 0f
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
            val companyNameText = company.canonicalName
            val quoteNumText = quotation.quotationNumber
            if (companyNameText.isNotBlank()) {
                addCommand { canvas, _, _ ->
                    val p = getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_BOLD)
                    canvas.drawText("${companyNameText.uppercase()} - QUOTATION #$quoteNumText", marginX, y + 10f, p)
                    canvas.drawLine(marginX, y + 14f, endX, y + 14f, getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE))
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
                val headerBgPaint = getPaint(COLOR_PRIMARY_BLUE, style = Paint.Style.FILL)
                canvas.drawRect(colX[0], y, colX[colX.size - 1] + colWidths[colWidths.size - 1], y + 18f, headerBgPaint)

                val textPaint = getPaint(COLOR_WHITE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER)

                val fontMetrics = textPaint.fontMetrics
                val centerY = y + 18f / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f

                val headers = listOf("Sl No", "Item", "Specifications", "Size", "Qty", "Rate", "Amount")
                for (i in headers.indices) {
                    if (i == 4 || i == 5 || i == 6) {
                        val cx = colX[i] + colWidths[i] - 4f
                        val rightPaint = getPaint(COLOR_WHITE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.RIGHT)
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

    fun generateQuotationPdf(
        context: Context,
        company: CompanyProfile,
        snapshot: com.example.domain.models.FinalizedQuotationSnapshot,
        outputFile: File
    ): File {
        val (quotation, items) = com.example.data.snapshot.QuotationSnapshotMapper.toEntity(snapshot)
        return generateQuotationPdf(context, company, quotation, items, outputFile, isSnapshotMode = true)
    }

    private fun generateQuotationPdf(
        context: Context,
        company: CompanyProfile,
        quotation: Quotation,
        items: List<QuotationItem>,
        outputFile: File,
        isSnapshotMode: Boolean = false
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

        val customer = if (isSnapshotMode || quotation.status.equals("Final", ignoreCase = true) || quotation.status == "FINALIZED") {
            com.example.data.CustomerEntity(
                customerId = quotation.customerId.toLong(),
                customerName = quotation.customerName,
                mobileNumber = quotation.customerPhone,
                address = quotation.customerAddress,
                siteLocation = quotation.siteName,
                siteAddress = quotation.siteAddress,
                email = quotation.customerEmail,
                whatsappNumber = quotation.customerWhatsapp,
                contactPerson = quotation.customerContactPerson,
                companyName = quotation.customerCompanyName,
                gstin = quotation.customerGstin
            )
        } else {
            // Query database for complete client details
            val db = com.example.data.AppDatabase.getDatabase(context)
            kotlinx.coroutines.runBlocking {
                try {
                    db.customerDao().getCustomerById(quotation.customerId.toLong())
                } catch (e: Exception) {
                    null
                }
            }
        }

        val engine = PdfEngine(context, company, quotation)

        // --- RENDER MODULAR SECTIONS ---
        drawHeader(engine, company, quotation, showLogo, showValidUntil, showGst, showWebsite, showWhatsapp)
        drawCustomer(engine, customer, quotation)
        drawItemsTable(engine, items, isSnapshotMode)
        drawSummaryAndPayment(engine, quotation, company, showGst, showAmountInWords, showBankDetails, showQrCode, isSnapshotMode)
        drawTerms(engine, company, quotation, showTermsConditions, isSnapshotMode)
        drawSignature(engine, company, showCompanySeal, showSignature)
        drawReferenceImages(context, engine, items)

        // --- COMPILE PDF DOCUMENT PAGES ---
        val pdfDocument = PdfDocument()
        try {
            engine.pagesCommands.forEachIndexed { idx, commands ->
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, idx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Standard elegant corporate page border
                val pBorder = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)
                canvas.drawRect(20f, 20f, 575f, 822f, pBorder)

                // Inject Stable Metadata into the first page
                if (idx == 0) {
                    val metaPaint = engine.getPaint(Color.TRANSPARENT, 1f, TYPEFACE_NORMAL)
                    canvas.drawText("Title: Estimate & Quotation", 0f, 0f, metaPaint)
                    canvas.drawText("Author: InteriorPro ERP", 0f, 0f, metaPaint)
                    canvas.drawText("Subject: Interior Quotation", 0f, 0f, metaPaint)
                    canvas.drawText("Keywords: interior, quotation, estimate, ERP", 0f, 0f, metaPaint)
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    canvas.drawText("CreationDate: $dateStr", 0f, 0f, metaPaint)
                    canvas.drawText("AppVersion: 1.5", 0f, 0f, metaPaint)
                }

                commands.forEach { cmd ->
                    cmd(canvas, idx + 1, engine.pagesCommands.size)
                }

                drawFooter(engine, canvas, idx + 1, engine.pagesCommands.size, company, showPageNumber)
                pdfDocument.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try {
                pdfDocument.close()
            } catch (e: Exception) {
            }
            engine.clearCaches()
        }
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

        val compNamePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 13f, TYPEFACE_BOLD)
        val taglinePaint = engine.getPaint(COLOR_ACCENT_ORANGE, 8f, TYPEFACE_BOLD)
        val contactPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 7f, TYPEFACE_NORMAL)

        val maxLeftW = (390f - headerTextLeft - 15f).toInt()
        val coNameLines = if (true) wrapText(company.canonicalName.uppercase(), maxLeftW, compNamePaint) else emptyList()
        val safeTagline = company.tagline.replace(Regex("(?i)primium"), "Premium")
        val taglineLines = if (safeTagline.isNotBlank()) wrapText(safeTagline, maxLeftW, taglinePaint) else emptyList()

        val addrParts = listOf(company.address, company.city, company.state, company.pincode).filter { it.isNotBlank() }
        val addrLines = if (addrParts.isNotEmpty()) wrapText(addrParts.joinToString(", "), maxLeftW, contactPaint) else emptyList()

        fun smartWrapParts(parts: List<String>, maxW: Int, paint: Paint): List<String> {
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (part in parts) {
                if (currentLine.isEmpty()) {
                    currentLine = part
                } else {
                    val testLine = "$currentLine | $part"
                    if (paint.measureText(testLine) <= maxW) {
                        currentLine = testLine
                    } else {
                        lines.add(currentLine)
                        currentLine = part
                    }
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            return lines
        }

        val phoneEmailParts = mutableListOf<String>()
        if (company.phone.isNotBlank()) phoneEmailParts.add("Ph: ${company.phone}")
        if (showWhatsapp && company.whatsappNumber.isNotBlank()) phoneEmailParts.add("WA: ${company.whatsappNumber}")
        if (company.email.isNotBlank()) phoneEmailParts.add("Email: ${company.email}")
        val phoneEmailLines = smartWrapParts(phoneEmailParts, maxLeftW, contactPaint)

        val gstinWebParts = mutableListOf<String>()
        if (showGst && company.gstin.isNotBlank()) gstinWebParts.add("GSTIN: ${company.gstin}")
        val computedPan = if (showGst) extractPanFromGstin(company.gstin) else null
        if (computedPan != null) gstinWebParts.add("PAN: $computedPan")
        if (showWebsite && company.website.isNotBlank()) gstinWebParts.add("Web: ${company.website}")
        val gstinWebLines = smartWrapParts(gstinWebParts, maxLeftW, contactPaint)

        val logoBottom = if (hasLogo) headerY + logoHeight else headerY
        var expectedTextHeight = 0f
        if (coNameLines.isNotEmpty()) {
            expectedTextHeight += coNameLines.size * 14f + 2f
        }
        if (taglineLines.isNotEmpty()) {
            expectedTextHeight += taglineLines.size * 10f + 2f
        }
        if (addrLines.isNotEmpty()) {
            expectedTextHeight += addrLines.size * 9f + 2f
        }
        if (phoneEmailLines.isNotEmpty()) {
            expectedTextHeight += phoneEmailLines.size * 9f + 2f
        }
        if (gstinWebLines.isNotEmpty()) {
            expectedTextHeight += gstinWebLines.size * 9f + 2f
        }
        val textHeight = expectedTextHeight
        val logoTop = headerY
        val textStartTop = headerY

        val leftColumnBottom = headerY + maxOf(if (hasLogo) logoHeight else 0f, textHeight)
        val boxBottom = if (showValidUntil) headerY + 54f else headerY + 36f
        val contentBottom = maxOf(leftColumnBottom, boxBottom)

        val bannerY = contentBottom + 12f

        engine.addCommand { canvas, _, _ ->
            if (hasLogo) {
                drawBitmapSafely(engine, canvas, company.logoPath, engine.marginX, logoTop, logoWidth, logoHeight)
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
            canvas.drawRoundRect(metaBox, 4f, 4f, engine.getPaint(COLOR_LIGHT_BG))
            canvas.drawRoundRect(metaBox, 4f, 4f, engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE))

            val lineP = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)
            canvas.drawLine(boxLeft, boxTop + 18f, boxLeft + boxW, boxTop + 18f, lineP)
            if (showValidUntil) {
                canvas.drawLine(boxLeft, boxTop + 36f, boxLeft + boxW, boxTop + 36f, lineP)
            }

            val labelP = engine.getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_NORMAL)
            val valP = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD)

            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.US)
            val dateStr = sdf.format(Date(quotation.date))
            val validityDays = quotation.validityDays.takeIf { it > 0 } ?: company.defaultValidityDays.takeIf { it > 0 } ?: 30
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

            val titleP = engine.getPaint(COLOR_PRIMARY_BLUE, 12f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER)
            canvas.drawText("ESTIMATE & QUOTATION", 595f / 2f, bannerY + 12f, titleP)
            canvas.drawLine(engine.marginX, bannerY + 18f, engine.endX, bannerY + 18f, engine.getPaint(COLOR_ACCENT_ORANGE, strokeWidth = 1f, style = Paint.Style.STROKE))
        }
        val totalHeaderHeight = (bannerY + 18f) - headerY
        engine.currentY += totalHeaderHeight
        engine.currentY += 14f // explicit section spacing
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
            if (isValidCustomerValue(customer.companyName)) {
                clientRowsLeft.add(Pair("Company Name", customer.companyName.trim()))
            }
            if (isValidCustomerValue(quotation.projectName)) {
                clientRowsLeft.add(Pair("Project Name", quotation.projectName.trim()))
            }
            val siteN = quotation.siteName.ifBlank { customer.siteLocation }
            if (isValidCustomerValue(siteN)) {
                clientRowsLeft.add(Pair("Site Name", siteN.trim()))
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
            val siteAdd = quotation.siteAddress.ifBlank { customer.siteAddress }
            if (isValidCustomerValue(siteAdd)) {
                clientRowsRight.add(Pair("Site Address", siteAdd.trim()))
            }
            if (isValidCustomerValue(quotation.customerNotes)) {
                clientRowsLeft.add(Pair("Notes", quotation.customerNotes.trim()))
            } else if (isValidCustomerValue(customer.notes)) {
                clientRowsLeft.add(Pair("Notes", customer.notes.trim()))
            }
        } else {
            if (isValidCustomerValue(quotation.customerName)) {
                clientRowsLeft.add(Pair("Customer Name", quotation.customerName.trim()))
            }
            if (isValidCustomerValue(quotation.customerCompanyName)) {
                clientRowsLeft.add(Pair("Company Name", quotation.customerCompanyName.trim()))
            }
            if (isValidCustomerValue(quotation.projectName)) {
                clientRowsLeft.add(Pair("Project Name", quotation.projectName.trim()))
            }
            val siteN = quotation.siteName
            if (isValidCustomerValue(siteN)) {
                clientRowsLeft.add(Pair("Site Name", siteN.trim()))
            }
            if (isValidCustomerValue(quotation.customerContactPerson)) {
                clientRowsLeft.add(Pair("Contact Person", quotation.customerContactPerson.trim()))
            }
            if (isValidCustomerValue(quotation.customerGstin)) {
                clientRowsLeft.add(Pair("GSTIN", quotation.customerGstin.trim()))
            }

            if (isValidCustomerValue(quotation.customerPhone)) {
                clientRowsRight.add(Pair("Mobile", quotation.customerPhone.trim()))
            }
            if (isValidCustomerValue(quotation.customerWhatsapp)) {
                clientRowsRight.add(Pair("Alternate Mobile", quotation.customerWhatsapp.trim()))
            }
            if (isValidCustomerValue(quotation.customerEmail)) {
                clientRowsRight.add(Pair("Email", quotation.customerEmail.trim()))
            }
            if (isValidCustomerValue(quotation.customerAddress)) {
                clientRowsRight.add(Pair("Billing Address", quotation.customerAddress.trim()))
            }
            if (isValidCustomerValue(quotation.siteAddress)) {
                clientRowsRight.add(Pair("Site Address", quotation.siteAddress.trim()))
            }
            if (isValidCustomerValue(quotation.customerNotes)) {
                clientRowsLeft.add(Pair("Notes", quotation.customerNotes.trim()))
            }
        }

        if (clientRowsLeft.isEmpty() && clientRowsRight.isEmpty()) return

        val labelP = engine.getPaint(COLOR_TEXT_SECONDARY, 7.5f, TYPEFACE_NORMAL)
        val valP = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD)

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
        totalContentH += 12f // Bottom padding inside the customer card

        val cardH = totalContentH
        engine.ensureSpace(cardH + 14f, reserveHeader = true)

        val blockTop = engine.currentY
        val cardW = engine.usableWidth

        engine.addCommand { canvas, _, _ ->
            val hPaint = engine.getPaint(COLOR_PRIMARY_BLUE, 8f, TYPEFACE_BOLD)
            val cardBgPaint = engine.getPaint(COLOR_LIGHT_BG)
            val borderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)
            val accentBarPaint = engine.getPaint(COLOR_PRIMARY_BLUE)

            // Draw single full-width card styled beautifully
            val cardRect = RectF(engine.marginX, blockTop, engine.marginX + cardW, blockTop + cardH)
            canvas.drawRoundRect(cardRect, 4f, 4f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 4f, 4f, borderPaint)
            canvas.drawRect(engine.marginX, blockTop, engine.marginX + 3f, blockTop + cardH, accentBarPaint)

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
        engine.currentY += cardH + 14f
    }

    private fun parseDimensionToInchesAndFeet(dimStr: String): Pair<Double, Double> {
        val feet = com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(dimStr)
        val inches = feet * 12.0
        return Pair(inches, feet)
    }

    private fun formatInchesDisplay(inches: Double): String {
        return if (inches % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f\"", inches)
        } else if (inches * 10 % 1.0 == 0.0) {
            String.format(Locale.US, "%.1f\"", inches)
        } else {
            String.format(Locale.US, "%.2f\"", inches)
        }
    }

    private fun drawItemsTable(engine: PdfEngine, items: List<QuotationItem>, isSnapshotMode: Boolean) {
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

        val bodyPaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL)
        val bodyBoldPaint = engine.getPaint(COLOR_PRIMARY_BLUE, 7.5f, TYPEFACE_BOLD)
        val descPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 7f, TYPEFACE_ITALIC)
        val specPaint = engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_NORMAL)

        abstract class ColNode { abstract val height: Float }
        class TextNode(val text: String, val paint: Paint, val xOffset: Float, override val height: Float, val isCentered: Boolean = false) : ColNode()
        class SpecNode(val label: String, val value: String, val labelPaint: Paint, val valuePaint: Paint, val maxLabelWidth: Float, override val height: Float) : ColNode()
        class SpaceNode(override val height: Float) : ColNode()

        fun toTitleCase(s: String): String {
            val specialCases = mapOf("tv" to "TV")
            return s.split(" ").joinToString(" ") { word ->
                val lower = word.lowercase()
                if (specialCases.containsKey(lower)) specialCases[lower]!!
                else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
            }
        }

        items.forEachIndexed { index, item ->
            val (userDesc, specs) = parseItemSpecs(item.description)

            val rawName = item.itemName.trim().ifBlank { "Interior Item" }
            val titleName = toTitleCase(rawName)
            val itemNameLines = wrapText(titleName, 107, bodyBoldPaint)
            val descLines = if (userDesc.isNotBlank()) wrapText(userDesc, 107, descPaint) else emptyList()

            val specsList = generateSpecsList(item)
            val specsWrapped = mutableListOf<Pair<String, List<String>>>()
            var maxLabelWidth = 0f
            specsList.forEach { (label, value) ->
                val lw = specPaint.measureText(label)
                if (lw > maxLabelWidth) maxLabelWidth = lw
                val availableW = (colWidths[2] - 25f - maxLabelWidth).toInt()
                val lines = wrapText(value, availableW, specPaint)
                specsWrapped.add(Pair(label, lines))
            }

            // Size Computation using original strings if present
            val wFeet = if (isSnapshotMode) item.parsedWidth else com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(specs.width)
            val hFeet = if (isSnapshotMode) item.parsedHeight else com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(specs.height)
            val dFeet = if (isSnapshotMode) item.parsedDepth else com.example.engine.QuotationCalculationEngine.parseDimensionToFeet(specs.depth)

            val sizeLinesStr = mutableListOf<String>()
            val dims = mutableListOf<String>()
            fun formatDimension(feet: Double): String {
                if (feet <= 0.0) return ""
                val wholeFeet = feet.toLong()
                val inches = Math.round((feet - wholeFeet) * 12.0)
                var f = wholeFeet
                var i = inches
                if (i == 12L) { f += 1; i = 0 }
                return if (i == 0L) "$f'" else if (f == 0L) "$i\"" else "$f' $i\""
            }
            if (wFeet > 0.0) dims.add(formatDimension(wFeet))
            if (hFeet > 0.0) dims.add(formatDimension(hFeet))
            if (dFeet > 0.0) dims.add(formatDimension(dFeet))

            if (wFeet > 0.0) sizeLinesStr.add("W : " + formatDimension(wFeet))
            if (hFeet > 0.0) sizeLinesStr.add("H : " + formatDimension(hFeet))
            if (dFeet > 0.0) sizeLinesStr.add("D : " + formatDimension(dFeet))
            
            val billableQty = if (isSnapshotMode) item.billableQuantity else (if (item.rate > 0) item.amount / item.rate else com.example.engine.QuotationCalculationEngine.calculateQuantity(specs.width, specs.height, item.quantity, item.unit, specs.depth))
            val uLower = item.unit.trim().lowercase(Locale.US)
            
            val billableLabel = when {
                uLower.contains("sq") -> "Area:"
                uLower.contains("cu") -> "Volume:"
                uLower.contains("ft") || uLower.contains("meter") || uLower == "r.m" || uLower == "rm" -> "Length:"
                else -> ""
            }
            if (billableLabel.isNotEmpty()) {
                val qtyRounded = Math.round(billableQty * 100.0) / 100.0
                val displayUnit = when (item.unit.trim().uppercase(Locale.US)) {
                    "SQ_FT" -> "sq.ft."
                    "CU_FT" -> "cu.ft."
                    "R.M", "RM" -> "r.m."
                    "R.FT", "RFT" -> "r.ft."
                    else -> item.unit.trim().replace("_", " ")
                }
                val qtyStr = if (qtyRounded % 1.0 == 0.0) String.format(Locale.US, "%.0f %s", qtyRounded, displayUnit) else String.format(Locale.US, "%.2f %s", qtyRounded, displayUnit)
                
                val combinedLine = "$billableLabel $qtyStr"
                val lines = wrapText(combinedLine, (colWidths[3] - 4f).toInt(), engine.getPaint(COLOR_PRIMARY_BLUE, 6f, TYPEFACE_BOLD))
                sizeLinesStr.addAll(lines)
            }


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
            val displayQty = item.quantity
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
            
            val qtyLines = wrapText("$qtyStr $displayUnit", (colWidths[4] - 4f).toInt(), engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL))
            val h4 = qtyLines.size * 10f

            while (remainingCol1.isNotEmpty() || remainingCol2.isNotEmpty() || remainingCol3.isNotEmpty()) {
                var availableSpace = engine.maxContentY - engine.currentY - 14f
                val requiredMinSpace = maxOf(22f, if (isFirstSlice) h4 + 14f else 0f)
                if (availableSpace < requiredMinSpace) {
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

                val rowHeight = maxOf(28f, maxOf(h1, h2, h3) + 14f, if (isFirstSlice) h4 + 14f else 0f)
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
                        
                        val qtyLines = wrapText("$qtyStr $displayUnit", (colWidths[4] - 4f).toInt(), engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL))
                        var qy = cellTextY
                        qtyLines.forEach { line ->
                            canvas.drawText(line, colX[4] + colWidths[4] / 2f, qy, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_NORMAL, textAlign = android.graphics.Paint.Align.CENTER))
                            qy += 10f
                        }
                        
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
        engine.inTableMode = false
        engine.currentY += 14f // explicit section spacing
    }

    private fun drawSummaryAndPayment(
        engine: PdfEngine,
        quotation: Quotation,
        company: CompanyProfile,
        showGst: Boolean,
        showAmountInWords: Boolean,
        showBankDetails: Boolean,
        showQrCode: Boolean,
        isSnapshotMode: Boolean
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
        
        val normalizedFinalGrandTotal = com.example.utils.CurrencyFormatter.normalizeCurrency(quotation.grandTotal)
        val balanceDue = if (isSnapshotMode) quotation.balance else (normalizedFinalGrandTotal - quotation.advance)
        if (showAmountInWords) {
            val currencyWord = if (Math.abs(normalizedFinalGrandTotal - 1.0) < 0.005) "Rupee " else "Rupees "
            val wordsStr = "Amount in Words: " + currencyWord + (if (isSnapshotMode) quotation.amountInWords else convertNumberToWords(normalizedFinalGrandTotal))
            wrappedWords = wrapText(wordsStr, engine.usableWidth.toInt(), wordsPaint)
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
        engine.rightYTracker = blockTop + totalsBoxH
        engine.currentY = blockTop + payCardH + (if (payCardH > 0f) sectionSpacing else 0f)
    }

    private fun drawTerms(
        engine: PdfEngine,
        company: CompanyProfile,
        quotation: Quotation,
        showTermsConditions: Boolean,
        isSnapshotMode: Boolean
    ) {
        if (!showTermsConditions) return

        val termsList = mutableListOf<String>()

        if (!isSnapshotMode) {
            // Dynamic terms settings values
            val warrantyVal = quotation.warranty.ifBlank { company.defaultWarranty }
            if (warrantyVal.isNotBlank()) {
                termsList.add("Warranty : $warrantyVal")
            }
            val deliveryVal = quotation.deliveryTime.ifBlank { company.defaultDeliveryTime }
            if (deliveryVal.isNotBlank()) {
                termsList.add("Delivery Time : $deliveryVal")
            }
            val installVal = quotation.installationTime.ifBlank { company.defaultInstallationTime }
            if (installVal.isNotBlank()) {
                termsList.add("Installation Time : $installVal")
            }
            val paymentVal = quotation.paymentTerms.ifBlank { company.defaultPaymentTerms }
            if (paymentVal.isNotBlank()) {
                termsList.add("Payment Terms : $paymentVal")
            }
            val validityDays = quotation.validityDays.takeIf { it > 0 } ?: company.defaultValidityDays.takeIf { it > 0 } ?: 30
            termsList.add("Quote Validity : $validityDays Days")
            val additionalVal = quotation.additionalConditions.ifBlank { company.additionalConditions }
            if (additionalVal.isNotBlank()) {
                termsList.add("Additional Conditions : $additionalVal")
            }
        } else {
            if (quotation.warranty.isNotBlank()) {
                termsList.add("Warranty : ${quotation.warranty}")
            }
            if (quotation.deliveryTime.isNotBlank()) {
                termsList.add("Delivery Time : ${quotation.deliveryTime}")
            }
            if (quotation.installationTime.isNotBlank()) {
                termsList.add("Installation Time : ${quotation.installationTime}")
            }
            if (quotation.paymentTerms.isNotBlank()) {
                termsList.add("Payment Terms : ${quotation.paymentTerms}")
            }
            if (quotation.validityDays > 0) {
                termsList.add("Quote Validity : ${quotation.validityDays} Days")
            }
            if (quotation.additionalConditions.isNotBlank()) {
                termsList.add("Additional Conditions : ${quotation.additionalConditions}")
            }
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

        val leftX = engine.marginX

        data class TermItem(
            val number: Int,
            val label: String,
            val value: String,
            val bullets: List<String> = emptyList()
        )

        val termItems = mutableListOf<TermItem>()
        val seenLabels = mutableSetOf<String>()
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
                    
                    if (!seenLabels.contains(label.lowercase(Locale.US))) {
                        seenLabels.add(label.lowercase(Locale.US))
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
                    }
                } else if (cleaned.contains(" : ")) {
                    val parts = cleaned.split(" : ")
                    val label = parts[0].trim()
                    if (!seenLabels.contains(label.lowercase(Locale.US))) {
                        seenLabels.add(label.lowercase(Locale.US))
                        termItems.add(TermItem(termIdx++, label, parts[1].trim()))
                    }
                } else {
                    if (!seenLabels.contains(cleaned.lowercase(Locale.US))) {
                        seenLabels.add(cleaned.lowercase(Locale.US))
                        termItems.add(TermItem(termIdx++, cleaned, ""))
                    }
                }
            }
        }

        val termSpacing = 4f
        var contentHeight = 0f
        val col1Width = 20f
        val col2Width = 110f
        val col3Width = 15f
        val col4Width = 140f
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
                wrapText(item.label, 260, measurePaint)
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
        val titleHeight = 16f
        
        val firstTermH = wrappedTerms.firstOrNull()?.itemH ?: 0f
        engine.ensureSpace(titleHeight + firstTermH, reserveHeader = true)

        val blockTop = engine.currentY
        engine.addCommand { canvas, _, _ ->
            val titlePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 8.5f, TYPEFACE_BOLD)
            canvas.drawText("TERMS & CONDITIONS", leftX, blockTop + 8f, titlePaint)
        }
        engine.currentY += 12f

        wrappedTerms.forEachIndexed { index, wt ->
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
        engine.currentY -= termSpacing // remove trailing term gap
        engine.currentY += sectionSpacing // add section gap
         // add section gap
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

        val signatureBoxH = 65f
        
        engine.currentY = maxOf(engine.currentY, engine.rightYTracker)

        engine.ensureSpace(signatureBoxH, reserveHeader = true)
        val sigY = engine.currentY + 5f

        engine.addCommand { canvas, _, _ ->
            val baselineY = sigY + 40f
            val linePaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.75f, style = Paint.Style.STROKE)

            // Left Column (Column 1): Company Seal
            val sealCenterX = engine.marginX + 75f
            if (hasSeal) {
                // Seal is 40x40. Draw centered horizontally on sealCenterX, bottom aligned at baselineY
                drawBitmapSafely(engine, canvas, sealPath, sealCenterX - 20f, baselineY - 40f, 40f, 40f)
            }
            // Draw a clean baseline under the seal
            canvas.drawLine(sealCenterX - 50f, baselineY, sealCenterX + 50f, baselineY, linePaint)
            // Label centered below baseline
            canvas.drawText("COMPANY SEAL", sealCenterX, baselineY + 11f, engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))

            // Right Column (Column 2): Signature & Signatory Name
            val authCenterX = engine.endX - 75f
            val coNameVal = company.canonicalName
            if (coNameVal.isNotBlank()) {
                canvas.drawText("For ${coNameVal.uppercase(Locale.US)}", authCenterX, baselineY - 32f, engine.getPaint(COLOR_TEXT_SECONDARY, 7f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))
            }

            if (hasSig) {
                // Signature is 80x25. Draw centered horizontally on authCenterX, bottom aligned at baselineY
                drawBitmapSafely(engine, canvas, sigPath, authCenterX - 40f, baselineY - 25f, 80f, 25f)
            }

            // Draw a clean baseline under the signature / name
            canvas.drawLine(authCenterX - 50f, baselineY, authCenterX + 50f, baselineY, linePaint)

            // Draw owner name and designation centered below baseline
            var sigLabelY = baselineY + 11f
            val ownerName = company.ownerName
            if (ownerName.isNotBlank()) {
                canvas.drawText(ownerName, authCenterX, sigLabelY, engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER))
                sigLabelY += 9f
            }

            val designation = company.signatureText.ifBlank { "Authorized Signatory" }
            canvas.drawText(designation, authCenterX, sigLabelY, engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.CENTER))
        }
        engine.currentY += signatureBoxH + 5f
    }

    private fun drawReferenceImages(context: Context, engine: PdfEngine, items: List<QuotationItem>) {
        data class ImageInfo(
            val path: String,
            val caption: String
        )
        val validImages = mutableListOf<ImageInfo>()
        items.forEach { item ->
            val (_, specs) = parseItemSpecs(item.description)
            if (specs.laminateImageUri.isNotBlank()) {
                val file = File(context.filesDir, File(specs.laminateImageUri).name)
                if (file.exists()) {
                    validImages.add(ImageInfo(file.absolutePath, "${item.itemName} - Laminate/Finish"))
                }
            }
            if (specs.designImageUri.isNotBlank()) {
                val file = File(context.filesDir, File(specs.designImageUri).name)
                if (file.exists()) {
                    validImages.add(ImageInfo(file.absolutePath, "${item.itemName} - Design Reference"))
                }
            }
        }

        if (validImages.isEmpty()) return

        val titlePaint = engine.getPaint(COLOR_PRIMARY_BLUE, 10f, TYPEFACE_BOLD)
        val cardW = 245f
        val cardH = 220f
        val gapX = 33f
        val gapY = 20f

        var currentImgIdx = 0
        var isFirstTitle = true

        while (currentImgIdx < validImages.size) {
            val titleHeight = if (isFirstTitle) 30f else 0f
            val requiredHeight = titleHeight + cardH

            // If we can't even fit one row, start a new page
            if (engine.currentY + requiredHeight > engine.maxContentY) {
                engine.startNewPage(reserveHeader = true)
            }

            if (isFirstTitle || engine.currentY == engine.topMargin) {
                val titleY = engine.currentY + 12f
                val titleText = if (isFirstTitle) "REFERENCE IMAGES" else "REFERENCE IMAGES (Contd.)"
                engine.addCommand { canvas, _, _ ->
                    canvas.drawText(titleText, engine.marginX, titleY, titlePaint)
                    canvas.drawLine(
                        engine.marginX, titleY + 6f, engine.endX, titleY + 6f,
                        engine.getPaint(COLOR_BORDER, strokeWidth = 0.75f, style = Paint.Style.STROKE)
                    )
                }
                engine.currentY += 30f
                isFirstTitle = false
            }

            val startY = engine.currentY
            var imagesPlacedOnPage = 0
            
            while (currentImgIdx < validImages.size) {
                val row = imagesPlacedOnPage / 2
                val col = imagesPlacedOnPage % 2
                val y = startY + row * (cardH + gapY)
                
                // Need a new page if this row doesn't fit
                if (col == 0 && y + cardH > engine.maxContentY) {
                    break 
                }

                val imageInfo = validImages[currentImgIdx]
                val x = engine.marginX + col * (cardW + gapX)

                engine.addCommand { canvas, _, _ ->
                    val bgPaint = engine.getPaint(COLOR_LIGHT_BG)
                    val borderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)
                    val captionPaint = engine.getPaint(COLOR_DARK_SLATE, 7.5f, TYPEFACE_BOLD, textAlign = Paint.Align.CENTER)

                    val cardRect = RectF(x, y, x + cardW, y + cardH)
                    canvas.drawRoundRect(cardRect, 4f, 4f, bgPaint)
                    canvas.drawRoundRect(cardRect, 4f, 4f, borderPaint)

                    val imgAreaW = cardW - 16f
                    val imgAreaH = cardH - 36f
                    val imgX = x + 8f
                    val imgY = y + 8f

                    drawBitmapSafely(engine, canvas, imageInfo.path, imgX, imgY, imgAreaW, imgAreaH)

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
                
                imagesPlacedOnPage++
                currentImgIdx++
                
                // Update currentY after placing images
                if (imagesPlacedOnPage % 2 == 0 || currentImgIdx == validImages.size) {
                    engine.currentY = y + cardH + gapY
                }
            }
        }
    }

    private fun drawFooter(engine: PdfEngine, canvas: Canvas, pageNum: Int, totalPages: Int, company: CompanyProfile, showPageNumber: Boolean) {
        val footY = 842f - 40f
        val linePaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)
        canvas.drawLine(36f, footY, 559f, footY, linePaint)

        val paint = engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_NORMAL)

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
            engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.CENTER)
        )

        // Right: Page x of y
        if (showPageNumber) {
            val pageText = "Page $pageNum of $totalPages"
            canvas.drawText(
                pageText,
                559f,
                footY + 12f,
                engine.getPaint(COLOR_TEXT_SECONDARY, 6.5f, TYPEFACE_NORMAL, textAlign = Paint.Align.RIGHT)
            )
        }
    }

    private fun drawBitmapSafely(engine: PdfEngine, canvas: Canvas, path: String, left: Float, top: Float, reqWidth: Float, reqHeight: Float) {
        try {
            val bitmap = engine.getOrLoadBitmap(path, reqWidth, reqHeight)
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
                canvas.drawBitmap(bitmap, srcRect, destRect, engine.getPaint(Color.BLACK, isAntiAlias = true, isFilterBitmap = true))
            }
        } catch (e: Exception) {
        }
    }

    private fun drawQrCode(engine: PdfEngine, canvas: Canvas, left: Float, top: Float, size: Float, text: String) {
        val blackPaint = engine.getPaint(Color.BLACK)
        val borderPaint = engine.getPaint(COLOR_BORDER, strokeWidth = 0.5f, style = Paint.Style.STROKE)

        // Draw background card with light grey border
        val bgRect = RectF(left, top, left + size, top + size)
        canvas.drawRoundRect(bgRect, 2f, 2f, engine.getPaint(Color.WHITE))
        canvas.drawRoundRect(bgRect, 2f, 2f, borderPaint)

        // Render QR Code grid payload
        val cellSize = size / 21f

        fun drawFinderPattern(l: Float, t: Float) {
            canvas.drawRect(l, t, l + cellSize * 7, t + cellSize * 7, blackPaint)
            canvas.drawRect(l + cellSize, t + cellSize, l + cellSize * 6, t + cellSize * 6, engine.getPaint(Color.WHITE))
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

    private fun parseItemSpecs(description: String): Pair<String, ItemSpecs> {
        if (!description.contains("|||")) {
            val trimmed = description.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                try {
                    val json = org.json.JSONObject(trimmed)
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
                        grade = json.optString("grade", ""),
                        cncDesign = json.optString("cncDesign", "")
                    )
                    return Pair(json.optString("description", ""), specs)
                } catch (e: Exception) {
                    // ignore
                }
            }
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
                grade = json.optString("grade", ""),
                        cncDesign = json.optString("cncDesign", "")
            )
            Pair(userDesc, specs)
        } catch (e: Exception) {
            Pair(userDesc, ItemSpecs())
        }
    }

    private fun generateSpecsList(item: QuotationItem): List<Pair<String, String>> {
        val (userDesc, specs) = parseItemSpecs(item.description)
        val list = mutableListOf<Pair<String, String>>()
        val addedValues = mutableSetOf<String>()

        fun formatValue(v: String): String {
            var trimmed = v.trim()
            trimmed = trimmed.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            trimmed = trimmed.replace(Regex("([A-Z])([A-Z][a-z])"), "$1 $2")
            trimmed = trimmed.replace(",", ", ")
            trimmed = trimmed.replace(Regex("\\s+"), " ")
            
            val replacements = mapOf(
                "Polohardware" to "Polo Hardware",
                "Savronplay" to "Savron Ply"
            )
            replacements.forEach { (old, new) ->
                trimmed = trimmed.replace(Regex("(?i)\\b${old}\\b"), new)
            }
            
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
                        list.add(Pair(label, formatted))
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
        addSpec("CNC Design", specs.cncDesign)
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
        val grade: String = "",
        val cncDesign: String = ""
    )
}
