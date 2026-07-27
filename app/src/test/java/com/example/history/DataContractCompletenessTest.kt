package com.example.history

import com.example.data.Quotation
import com.example.data.QuotationItem
import com.example.domain.models.FinalizedQuotationSnapshot
import com.example.data.snapshot.QuotationSnapshotMapper
import org.junit.Assert.*
import org.junit.Test

class DataContractCompletenessTest {

    @Test
    fun testSnapshotContainsItemFinancials() {
        val q = Quotation(id = 1)
        val item = QuotationItem(
            id = 1,
            quotationId = 1,
            parsedWidth = 10.0,
            parsedHeight = 10.0,
            parsedDepth = 0.0,
            quantity = 100.0,
            unit = "SQ_FT",
            billableQuantity = 100.0,
            rate = 50.0,
            amount = 5000.0
        )
        
        val snapshot = QuotationSnapshotMapper.toDomain(q, listOf(item))
        val firstItem = snapshot.items.first()
        
        assertEquals(10.0, firstItem.parsedWidth, 0.01)
        assertEquals(100.0, firstItem.billableQuantity, 0.01)
        assertEquals(5000.0, firstItem.itemAmount, 0.01)
    }
}
