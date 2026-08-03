package com.example.pdf

import android.graphics.Paint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfMultilineRenderingTest {

    @Test
    fun test1_SingleLine() {
        val paint = Paint()
        paint.textSize = 10f
        val text = "Short text"
        val width = 1000 // Very wide, no wrapping needed
        val result = PdfGenerator.wrapText(text, width, paint)
        assertEquals(listOf("Short text"), result)
    }

    @Test
    fun test2_ExplicitNewline() {
        val paint = Paint()
        paint.textSize = 10f
        val text = "Line 1\nLine 2\nLine 3"
        val width = 1000
        val result = PdfGenerator.wrapText(text, width, paint)
        assertEquals(listOf("Line 1", "Line 2", "Line 3"), result)
    }

    @Test
    fun test3_LongWrappedParagraph() {
        val paint = Paint()
        paint.textSize = 10f
        // Assuming measureText is proportional or 1 character = some width
        // Robolectric Paint usually returns character count or similar, or we can mock/stub
        // Let's use a narrow width so it's forced to wrap
        val text = "This is a very long paragraph that should wrap into multiple lines."
        val width = 50 // narrow width
        val result = PdfGenerator.wrapText(text, width, paint)
        // Ensure it breaks into multiple lines (more than 1)
        assert(result.size > 1)
    }

    @Test
    fun test4_MixedNewlineAndWrap() {
        val paint = Paint()
        paint.textSize = 10f
        val text = "First short line\nAnd then a very long second line that will need wrapping"
        val width = 50 // narrow width
        val result = PdfGenerator.wrapText(text, width, paint)
        assert(result.size > 2)
        assertEquals("First short line", result.first())
    }

    @Test
    fun test5_EmptyLinesPreserved() {
        val paint = Paint()
        paint.textSize = 10f
        val text = "Line 1\n\nLine 2"
        val width = 1000
        val result = PdfGenerator.wrapText(text, width, paint)
        assertEquals(listOf("Line 1", "", "Line 2"), result)
    }
}
