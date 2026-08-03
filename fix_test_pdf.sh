cat << 'INNEREOF' > app/src/test/java/com/example/pdf/DummyPdfEngine.kt
package com.example.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.OutputStream

class DummyPdfEngine : IPdfEngine {
    override fun beginPage(width: Int, height: Int, pageNumber: Int): Canvas {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return Canvas(bitmap)
    }
    override fun endPage() {}
    override fun writeTo(out: OutputStream) {
        out.write("DUMMY_PDF_CONTENT".toByteArray())
    }
    override fun close() {}
}
INNEREOF
sed -i '/@Before/i \
    @org.junit.Before\
    fun setupPdfEngine() {\
        com.example.pdf.PdfGenerator.pdfEngineFactory = { com.example.pdf.DummyPdfEngine() }\
    }' app/src/test/java/com/example/pdf/ProductionPdfFlowTest.kt
