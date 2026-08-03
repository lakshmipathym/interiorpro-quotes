cat << 'INNEREOF' > app/src/main/java/com/example/pdf/IPdfEngine.kt
package com.example.pdf

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

interface IPdfEngine {
    fun beginPage(width: Int, height: Int, pageNumber: Int): Canvas
    fun endPage()
    fun writeTo(out: OutputStream)
    fun close()
}

class AndroidPdfEngine : IPdfEngine {
    private val doc = PdfDocument()
    private var currentPage: PdfDocument.Page? = null

    override fun beginPage(width: Int, height: Int, pageNumber: Int): Canvas {
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        currentPage = doc.startPage(pageInfo)
        return currentPage!!.canvas
    }

    override fun endPage() {
        currentPage?.let { doc.finishPage(it) }
        currentPage = null
    }

    override fun writeTo(out: OutputStream) {
        doc.writeTo(out)
    }

    override fun close() {
        doc.close()
    }
}
INNEREOF
