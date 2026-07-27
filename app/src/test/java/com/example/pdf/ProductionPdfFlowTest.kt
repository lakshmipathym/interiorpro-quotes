package com.example.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.models.*
import com.example.utils.ShareManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductionPdfFlowTest {

    @Test
    fun testProductionPdfFlowWithSnapshot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val quotesRepo = QuotesRepository(database)
        val snapshotRepo = QuotationSnapshotRepositoryImpl(database, quotesRepo)

        // Seed basic data
        val customer = CustomerEntity(customerName = "Test Cust", mobileNumber = "12345", address = "Address")
        database.customerDao().insertCustomer(customer)
        
        val quotation = Quotation(
            quotationNumber = "IPQ/TEST/001",
            customerId = 1,
            customerName = "Test Cust",
            subtotal = 1000.0,
            grandTotal = 1180.0
        )
        database.quotationDao().insertQuotation(quotation)
        
        val quotationItem = QuotationItem(
            quotationId = 1,
            itemName = "Item 1",
            unit = "Sq.Ft",
            amount = 1000.0,
            quantity = 1.0,
            rate = 1000.0,
            rawQuantity = 1.0,
            billableQuantity = 1.0
        )
        database.quotationItemDao().insertAll(listOf(quotationItem))
        
        // Finalize snapshot explicitly
        val snapshot = FinalizedQuotationSnapshot(
            id = "1",
            quotationNumber = "IPQ/TEST/001",
            date = 1L,
            customer = CustomerSnapshot("1", "Test Cust", "12345", "Address", "Site", "Site Addr"),
            company = CompanySnapshot("C", "O", "P", "E", "A", "G", "B", "AH", "AN", "I", "B", "U"),
            items = listOf(
                FinalizedItemSnapshot(
                    itemId = "1",
                    itemName = "Item 1",
                    description = "{}",
                    material = "M",
                    finish = "F",
                    rawWidth = "1",
                    rawHeight = "1",
                    rawDepth = "1",
                    parsedWidth = 1.0,
                    parsedHeight = 1.0,
                    parsedDepth = 1.0,
                    parsedUnit = UnitType.SQ_FT,
                    quantity = 2.0,
                    billableQuantity = 12.50,
                    rate = 100.0,
                    itemAmount = 1350.0
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
        snapshotRepo.saveSnapshot(snapshot)

        // Call ShareManager
        val file = ShareManager.generateQuotationPdf(context, quotesRepo, 1)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun testLegacyFallbackPdfFlow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = AppDatabase.getDatabase(context)
        val quotesRepo = QuotesRepository(database)

        // Empty database, insert a quotation directly without snapshot
        val quotation = Quotation(
            id = 999,
            quotationNumber = "IPQ/TEST/002",
            customerId = 2,
            customerName = "Legacy Cust",
            subtotal = 2000.0,
            grandTotal = 2360.0
        )
        database.quotationDao().insertQuotation(quotation)
        
        val quotationItem = QuotationItem(
            quotationId = 999,
            itemName = "Item 2",
            unit = "Sq.Ft",
            amount = 1000.0,
            quantity = 1.0,
            rate = 1000.0,
            rawQuantity = 1.0,
            billableQuantity = 1.0
        )
        database.quotationItemDao().insertAll(listOf(quotationItem))

        // Call ShareManager
        val file = ShareManager.generateQuotationPdf(context, quotesRepo, 999)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
