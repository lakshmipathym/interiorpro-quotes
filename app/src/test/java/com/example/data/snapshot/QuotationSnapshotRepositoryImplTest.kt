package com.example.data.snapshot

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuotationSnapshotRepositoryImplTest {

    private lateinit var classUnderTest: QuotationSnapshotRepositoryImpl
    private lateinit var db: AppDatabase
    private lateinit var quotesRepository: QuotesRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        quotesRepository = QuotesRepository(db)
        classUnderTest = QuotationSnapshotRepositoryImpl(db, quotesRepository)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test Domain Snapshot to Entity to Database to Entity to Domain Snapshot`() = runBlocking {
        val originalSnapshot = FinalizedQuotationSnapshot(
            id = "0",
            quotationNumber = "IPQ/2026/0001",
            date = 1672531200000L,
            customer = CustomerSnapshot(
                customerId = "1",
                customerName = "John Doe",
                customerPhone = "+1234567890",
                customerAddress = "123 Main St",
                siteName = "John's Villa",
                siteAddress = "456 Villa Rd"
            ),
            company = CompanySnapshot(
                companyName = "Interior Pro",
                ownerName = "Jane Smith",
                phone = "9876543210",
                email = "info@interiorpro.com",
                address = "789 Business Ave",
                gstin = "GST123456789",
                bankName = "Test Bank",
                accountHolderName = "Interior Pro Ltd",
                accountNumber = "123456789",
                ifsc = "TEST0001234",
                branch = "Main Branch",
                upiId = "interiorpro@upi"
            ),
            items = listOf(
                FinalizedItemSnapshot(
                    itemId = "0",
                    itemName = "Item 1",
                    description = "Description 1",
                    material = "Plywood",
                    finish = "Laminate",
                    rawWidth = "10",
                    rawHeight = "5",
                    rawDepth = "2",
                    parsedWidth = 10.0,
                    parsedHeight = 5.0,
                    parsedDepth = 2.0,
                    parsedUnit = UnitType.SQ_FT,
                    quantity = 1.0,
                    billableQuantity = 50.0,
                    rate = 1000.0,
                    itemAmount = 50000.0
                ),
                FinalizedItemSnapshot(
                    itemId = "0",
                    itemName = "Item 2",
                    description = "Description 2",
                    material = "MDF",
                    finish = "Paint",
                    rawWidth = "8",
                    rawHeight = "4",
                    rawDepth = "1",
                    parsedWidth = 8.0,
                    parsedHeight = 4.0,
                    parsedDepth = 1.0,
                    parsedUnit = UnitType.SQ_FT,
                    quantity = 2.0,
                    billableQuantity = 64.0,
                    rate = 1500.0,
                    itemAmount = 96000.0
                )
            ),
            financial = FinancialSnapshot(
                subtotal = 146000.0,
                discount = 6000.0,
                taxableAmount = 140000.0,
                gstRate = 18.0,
                gstAmount = 25200.0,
                transport = 1000.0,
                installation = 5000.0,
                extraCharges = 500.0,
                roundOff = 0.5,
                grandTotal = 171700.5,
                advance = 50000.0,
                balanceDue = 121700.5,
                amountInWords = "Rupees One Lakh Seventy One Thousand Seven Hundred and Fifty Paise Only"
            ),
            termsAndConditions = "Test Terms",
            warranty = "1 Year",
            validityDays = 30,
            notes = "Test Notes"
        )

        classUnderTest.saveSnapshot(originalSnapshot)
        
        // Since we saved with ID 0, it auto-generates. We can get it by quotationNumber
        val loadedSnapshot = classUnderTest.getSnapshotByNumber("IPQ/2026/0001")
        
        assertNotNull(loadedSnapshot)
        
        // Verify identity
        assertEquals(originalSnapshot.quotationNumber, loadedSnapshot!!.quotationNumber)
        assertEquals(originalSnapshot.date, loadedSnapshot.date)
        
        // Verify customer
        assertEquals(originalSnapshot.customer.customerName, loadedSnapshot.customer.customerName)
        assertEquals(originalSnapshot.customer.customerPhone, loadedSnapshot.customer.customerPhone)
        
        // Verify company
        assertEquals(originalSnapshot.company.companyName, loadedSnapshot.company.companyName)
        assertEquals(originalSnapshot.company.gstin, loadedSnapshot.company.gstin)
        
        // Verify financial
        assertEquals(originalSnapshot.financial.subtotal, loadedSnapshot.financial.subtotal, 0.0)
        assertEquals(originalSnapshot.financial.grandTotal, loadedSnapshot.financial.grandTotal, 0.0)
        assertEquals(originalSnapshot.financial.amountInWords, loadedSnapshot.financial.amountInWords)
        
        // Verify items
        assertEquals(2, loadedSnapshot.items.size)
        val firstItem = loadedSnapshot.items[0]
        assertEquals("Item 1", firstItem.itemName)
        assertEquals(50.0, firstItem.billableQuantity, 0.0)
        assertEquals(1000.0, firstItem.rate, 0.0)
        assertEquals(50000.0, firstItem.itemAmount, 0.0)
        assertEquals(UnitType.SQ_FT, firstItem.parsedUnit)
    }
}
