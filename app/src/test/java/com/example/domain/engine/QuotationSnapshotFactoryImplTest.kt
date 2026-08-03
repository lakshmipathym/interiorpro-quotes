package com.example.domain.engine

import com.example.domain.models.CalculatedItem
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.CompanySnapshot
import com.example.domain.models.CustomerSnapshot
import com.example.domain.models.RawItemInput
import com.example.domain.models.RawQuotationInput
import com.example.domain.models.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class QuotationSnapshotFactoryImplTest {

    private val factory = QuotationSnapshotFactoryImpl()

    @Test
    fun testSnapshotCreationPreservesValuesAndIsDeterministic() {
        val customer = CustomerSnapshot(
            customerId = "CUST1",
            customerName = "John Doe",
            customerPhone = "1234567890",
            customerAddress = "123 Main St",
            siteName = "Home",
            siteAddress = "456 Side St"
        )
        val company = CompanySnapshot(
            companyName = "Interior Pro",
            ownerName = "Jane Doe",
            phone = "0987654321",
            email = "info@interior.com",
            address = "789 Office St",
            gstin = "GST123",
            bankName = "Bank",
            accountHolderName = "Jane Doe",
            accountNumber = "123456",
            ifsc = "IFSC001",
            branch = "Main Branch",
            upiId = "jane@upi"
        )

        val rawInput = RawQuotationInput(
            discount = 100.0,
            gstRate = 18.0,
            transport = 50.0,
            installation = 50.0,
            extraCharges = 20.0,
            roundOff = 0.5,
            advance = 500.0
        )

        val rawItem = RawItemInput(
            itemName = "Wardrobe",
            description = "Master Bedroom",
            material = "Plywood",
            finish = "Laminate",
            width = "10",
            height = "10",
            depth = "2",
            quantity = 1.0,
            unit = "Sq.Ft",
            rate = 1500.0
        )

        val calcItem = CalculatedItem(
            rawInput = rawItem,
            parsedWidth = 10.0,
            parsedHeight = 10.0,
            parsedDepth = 2.0,
            parsedUnit = UnitType.SQ_FT,
            billableQuantity = 100.0,
            itemAmount = 150000.0
        )

        val calcQuotation = CalculatedQuotation(
            items = listOf(calcItem),
            subtotal = 150000.0,
            taxableAmount = 149900.0,
            gstAmount = 26982.0,
            grandTotal = 177002.5,
            balanceDue = 176502.5,
            amountInWords = "One Lakh Seventy Seven Thousand and Two and Fifty Paise Only"
        )

        val snapshot1 = factory.createSnapshot(
            id = "SNAP1",
            quotationNumber = "QT-001",
            date = 1600000000000L,
            customer = customer,
            company = company,
            termsAndConditions = "Terms",
            warranty = "1 Year",
            deliveryTime = "7 Days",
            installationTime = "2 Days",
            paymentTerms = "50% Advance",
            additionalConditions = "None",
            validityDays = 30,
            notes = "Notes",
            rawInput = rawInput,
            calculatedQuotation = calcQuotation
        )

        val snapshot2 = factory.createSnapshot(
            id = "SNAP1",
            quotationNumber = "QT-001",
            date = 1600000000000L,
            customer = customer,
            company = company,
            termsAndConditions = "Terms",
            warranty = "1 Year",
            deliveryTime = "7 Days",
            installationTime = "2 Days",
            paymentTerms = "50% Advance",
            additionalConditions = "None",
            validityDays = 30,
            notes = "Notes",
            rawInput = rawInput,
            calculatedQuotation = calcQuotation
        )

        assertEquals("SNAP1", snapshot1.id)
        assertEquals("QT-001", snapshot1.quotationNumber)
        
        // Check customer
        assertEquals("John Doe", snapshot1.customer.customerName)
        
        // Check company
        assertEquals("Interior Pro", snapshot1.company.companyName)

        // Check item - Exact Item Result Preservation
        assertEquals(1, snapshot1.items.size)
        val snapItem = snapshot1.items[0]
        assertEquals("Wardrobe", snapItem.itemName)
        assertEquals("Master Bedroom", snapItem.description) // Explicit regression check
        assertEquals(10.0, snapItem.parsedWidth, 0.001)
        assertEquals(1.0, snapItem.quantity, 0.001)
        assertEquals(100.0, snapItem.billableQuantity, 0.001)
        assertEquals(1500.0, snapItem.rate, 0.001)
        assertEquals(150000.0, snapItem.itemAmount, 0.001)

        // Check financial - Exact Financial Result Preservation
        val fin = snapshot1.financial
        assertEquals(150000.0, fin.subtotal, 0.001)
        assertEquals(100.0, fin.discount, 0.001)
        assertEquals(149900.0, fin.taxableAmount, 0.001)
        assertEquals(18.0, fin.gstRate, 0.001)
        assertEquals(26982.0, fin.gstAmount, 0.001)
        assertEquals(50.0, fin.transport, 0.001)
        assertEquals(50.0, fin.installation, 0.001)
        assertEquals(20.0, fin.extraCharges, 0.001)
        assertEquals(0.5, fin.roundOff, 0.001)
        assertEquals(177002.5, fin.grandTotal, 0.001)
        assertEquals("One Lakh Seventy Seven Thousand and Two and Fifty Paise Only", fin.amountInWords)

        // Deterministic check
        assertEquals(snapshot1.id, snapshot2.id)
        assertEquals(snapshot1.quotationNumber, snapshot2.quotationNumber)
        assertEquals(snapshot1.financial.grandTotal, snapshot2.financial.grandTotal, 0.001)

        // Ensure collection safety
        assertNotSame(calcQuotation.items, snapshot1.items)
    }
}
