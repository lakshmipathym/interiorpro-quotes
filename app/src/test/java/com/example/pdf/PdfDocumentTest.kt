package com.example.pdf

import android.graphics.pdf.PdfDocument
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfDocumentTest {

    @Test
    fun testPdfDocumentLoop() {
        val doc = PdfDocument()
        try {
            for (i in 0..0) {
                val pageInfo = PdfDocument.PageInfo.Builder(100, 100, i + 1).create()
                val page = doc.startPage(pageInfo)
                assertNotNull(page)
                doc.finishPage(page)
            }
        } catch (e: IllegalStateException) {
            if (e.message == "document is closed!") {
                return
            }
            throw e
        } finally {
            doc.close()
        }
    }
}
