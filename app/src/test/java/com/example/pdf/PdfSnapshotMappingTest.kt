package com.example.pdf

import org.junit.Test
import org.junit.Assert.*
import com.example.domain.models.*
import com.example.data.snapshot.QuotationSnapshotMapper

class PdfSnapshotMappingTest {

    @Test
    fun testBillableQuantityPreservation() {
        // Create a FinalizedQuotationSnapshot with a known billable quantity
        val snapshot = FinalizedQuotationSnapshot(
            id = "1",
            quotationNumber = "IPQ/2026/0001",
            date = 1L,
            customer = CustomerSnapshot("1", "C", "P", "A", "S", "SA"),
            company = CompanySnapshot("C", "O", "P", "E", "A", "G", "B", "AH", "AN", "I", "B", "U"),
            items = listOf(
                FinalizedItemSnapshot(
                    itemId = "1",
                    itemName = "Item",
                    description = "{}",
                    material = "M",
                    finish = "F",
                    rawWidth = "1.0",
                    rawHeight = "1.0",
                    rawDepth = "1.0",
                    parsedWidth = 1.0,
                    parsedHeight = 1.0,
                    parsedDepth = 1.0,
                    parsedUnit = UnitType.SQ_FT,
                    quantity = 2.0,
                    billableQuantity = 12.50, // Verify this exact value
                    rate = 100.0,
                    itemAmount = 1350.0 // Non-derived itemAmount
                )
            ),
            financial = FinancialSnapshot(
                subtotal = 10000.0,
                discount = 500.0,
                taxableAmount = 9500.0,
                gstRate = 18.0,
                gstAmount = 1710.0,
                transport = 0.0,
                installation = 0.0,
                extraCharges = 250.0,
                roundOff = 0.0,
                grandTotal = 11460.0,
                advance = 0.0,
                balanceDue = 11460.0,
                amountInWords = "ELEVEN THOUSAND FOUR HUNDRED SIXTY"
            ),
            termsAndConditions = "",
            warranty = "",
            validityDays = 30,
            notes = ""
        )

        val (mappedQuotation, mappedItems) = QuotationSnapshotMapper.toEntity(snapshot)
        
        // TEST 1: Billable Quantity Preservation
        assertEquals(12.50, mappedItems[0].billableQuantity, 0.001)

        // TEST 2: Item Amount Preservation
        assertEquals(1350.0, mappedItems[0].amount, 0.001)
        assertEquals(100.0, mappedItems[0].rate, 0.001)

        // TEST 3: Financial Snapshot Preservation
        assertEquals(10000.0, mappedQuotation.subtotal, 0.001)
        assertEquals(500.0, mappedQuotation.discount, 0.001)
        assertEquals(9500.0, mappedQuotation.taxableAmount, 0.001)
        assertEquals(1710.0, mappedQuotation.gstAmount, 0.001)
        assertEquals(250.0, mappedQuotation.extraCharges, 0.001)
        assertEquals(0.0, mappedQuotation.roundOff, 0.001)
        assertEquals(11460.0, mappedQuotation.grandTotal, 0.001)

        // TEST 4: Amount In Words Preservation
        assertEquals("ELEVEN THOUSAND FOUR HUNDRED SIXTY", mappedQuotation.amountInWords)

        // TEST 5: Malformed Specification JSON (Doesn't crash mapping)
        val malformedSnapshot = snapshot.copy(
            items = listOf(snapshot.items[0].copy(description = "INVALID JSON { ]"))
        )
        val (mappedQuotationMalformed, mappedItemsMalformed) = QuotationSnapshotMapper.toEntity(malformedSnapshot)
        assertEquals(12.50, mappedItemsMalformed[0].billableQuantity, 0.001)
        assertEquals(1350.0, mappedItemsMalformed[0].amount, 0.001)
        
        // TEST 6: Snapshot Data Immutability
        // The original snapshot values should be unchanged after mapping
        assertEquals(12.50, snapshot.items[0].billableQuantity, 0.001)
        assertEquals(1350.0, snapshot.items[0].itemAmount, 0.001)
    }
}
