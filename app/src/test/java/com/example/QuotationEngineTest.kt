package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.quotation.QuotationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuotationEngineTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Build in-memory database with synchronous executors for fast, deterministic unit testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testQuotationCalculationsAndDatabaseOperations() = runTest(testDispatcher) {
        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val app = context as Application
            val repository = QuotesRepository(database)
            val viewModel = QuotationViewModel(app, repository)

            // Start background collection of StateFlows to activate SharingStarted.WhileSubscribed
            val subtotalJob = launch { viewModel.newQuoteSubtotal.collect {} }
            val gstJob = launch { viewModel.newQuoteGstAmount.collect {} }
            val grandTotalJob = launch { viewModel.newQuoteGrandTotal.collect {} }

            // 1. Verify generation of unique quotation number
            viewModel.startNewQuotation()
            testScheduler.advanceUntilIdle()
            
            val quoteNum = viewModel.newQuoteNumber.value
            System.out.println("Generated Quote Num: $quoteNum")
            assertNotNull(quoteNum)
            assertTrue("Quote number should start with IPQ/: $quoteNum", quoteNum.startsWith("IPQ/"))

            // 2. Select customer
            val customerId = repository.saveCustomer(
                CustomerEntity(
                    customerName = "Pathy Contractor",
                    mobileNumber = "9876543210",
                    address = "123 Interior St, Bangalore"
                )
            )
            val customer = repository.getCustomerById(customerId)
            assertNotNull(customer)
            viewModel.selectCustomer(customer!!)

            // 3. Add Quotation Items
            val item1 = QuotationItem(
                quotationId = 0,
                itemName = "Modern Wardrobe",
                description = "3-door modular wardrobe with sliding glass",
                material = "MDF",
                finish = "Glossy",
                quantity = 2.0,
                unit = "Sq.Ft",
                rate = 1200.0,
                amount = 2400.0
            )
            viewModel.addQuoteItem(item1)

            val item2 = QuotationItem(
                quotationId = 0,
                itemName = "Kitchen Cabinets",
                description = "Top cabinets with hydraulic hinges",
                material = "Plywood",
                finish = "Laminate",
                quantity = 5.0,
                unit = "Rft",
                rate = 1500.0,
                amount = 7500.0
            )
            viewModel.addQuoteItem(item2)
            testScheduler.advanceUntilIdle()

            // Verify totals are calculated instantly
            var subtotal = viewModel.newQuoteSubtotal.value
            System.out.println("Calculated Subtotal: $subtotal")
            assertEquals(9900.0, subtotal, 0.001) // 2400 + 7500

            // Set Discount and GST Rate
            viewModel.setDiscount(900.0)
            viewModel.setGstRate(18.0)
            testScheduler.advanceUntilIdle()

            // Subtotal - Discount = 9000. GST (18% of 9000) = 1620. Grand Total = 10620.
            val gstAmount = viewModel.newQuoteGstAmount.value
            System.out.println("Calculated GST Amount: $gstAmount")
            assertEquals(1620.0, gstAmount, 0.001)

            val grandTotal = viewModel.newQuoteGrandTotal.value
            System.out.println("Calculated Grand Total: $grandTotal")
            assertEquals(10620.0, grandTotal, 0.001)

            // 4. Save Quotation and test Room relations
            var completedId = 0
            viewModel.saveQuotation { id ->
                completedId = id
            }
            testScheduler.advanceUntilIdle()
            
            System.out.println("Completed ID: $completedId")
            assertTrue("Expected completedId to be > 0", completedId > 0)

            val savedQuote = repository.getQuotationByIdDirect(completedId)
            assertNotNull(savedQuote)
            assertEquals("Pathy Contractor", savedQuote?.customerName)
            assertEquals(9900.0, savedQuote?.subtotal ?: 0.0, 0.001)
            assertEquals(900.0, savedQuote?.discount ?: 0.0, 0.001)
            assertEquals(1620.0, savedQuote?.gstAmount ?: 0.0, 0.001)
            assertEquals(10620.0, savedQuote?.grandTotal ?: 0.0, 0.001)

            val savedItems = repository.getQuotationItemsDirect(completedId)
            assertEquals(2, savedItems.size)
            assertEquals("Modern Wardrobe", savedItems[0].itemName)
            assertEquals("Kitchen Cabinets", savedItems[1].itemName)

            // 5. Test Editing Quotation
            viewModel.startNewQuotation() // reset
            testScheduler.advanceUntilIdle()
            
            val resetQuoteNum = viewModel.newQuoteNumber.value
            System.out.println("Reset Quote Num: $resetQuoteNum")

            viewModel.loadQuotationToEdit(savedQuote!!)
            testScheduler.advanceUntilIdle()

            // Verify fields loaded correctly (instant since all execution is unconfined & synchronous)
            assertEquals(quoteNum, viewModel.newQuoteNumber.value)
            assertEquals(2, viewModel.newQuoteItems.value.size)
            assertEquals(900.0, viewModel.newQuoteDiscount.value, 0.001)

            // Edit second item
            viewModel.updateQuoteItem(
                index = 1,
                updated = savedItems[1].copy(quantity = 6.0, amount = 9000.0) // 6 * 1500
            )
            testScheduler.advanceUntilIdle()

            // Verify updated totals instantly
            subtotal = viewModel.newQuoteSubtotal.value
            assertEquals(11400.0, subtotal, 0.001) // 2400 + 9000

            // Clean up StateFlow collector jobs
            subtotalJob.cancel()
            gstJob.cancel()
            grandTotalJob.cancel()
        } catch (t: Throwable) {
            System.out.println("TEST FAILURE ENCOUNTERED:")
            t.printStackTrace(System.out)
            throw t
        }
    }

    @Test
    fun testQuotationEditDuplicateAndStatus() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context as Application
        val repository = QuotesRepository(database)
        val quotationViewModel = QuotationViewModel(app, repository)
        val historyViewModel = com.example.ui.history.HistoryViewModel(app, repository)

        val subtotalJob = launch { quotationViewModel.newQuoteSubtotal.collect {} }
        val gstJob = launch { quotationViewModel.newQuoteGstAmount.collect {} }
        val grandTotalJob = launch { quotationViewModel.newQuoteGrandTotal.collect {} }

        // Create a quote
        quotationViewModel.startNewQuotation()
        testScheduler.advanceUntilIdle()

        val customerId = repository.saveCustomer(
            CustomerEntity(
                customerName = "Rohan Kumar",
                mobileNumber = "9988776655",
                address = "456 Design Blvd, Mumbai"
            )
        )
        val customer = repository.getCustomerById(customerId)
        assertNotNull(customer)
        quotationViewModel.selectCustomer(customer!!)

        val item = QuotationItem(
            quotationId = 0,
            itemName = "Laminate Console Table",
            description = "Entryway console table with metal legs",
            material = "Plywood",
            finish = "Laminate",
            quantity = 1.0,
            unit = "No",
            rate = 15000.0,
            amount = 15000.0
        )
        quotationViewModel.addQuoteItem(item)
        testScheduler.advanceUntilIdle()

        var savedId = 0
        quotationViewModel.saveQuotation { id ->
            savedId = id
        }
        testScheduler.advanceUntilIdle()
        assertTrue(savedId > 0)

        val savedQuote = repository.getQuotationByIdDirect(savedId)
        assertNotNull(savedQuote)
        assertEquals("Draft", savedQuote?.status) // Starts with "Draft"

        // 1. Test update status
        historyViewModel.updateQuotationStatus(savedId, "Final")
        testScheduler.advanceUntilIdle()

        val updatedQuote = repository.getQuotationByIdDirect(savedId)
        assertEquals("Final", updatedQuote?.status)

        // 2. Test edit preserves ID (no duplicates created)
        quotationViewModel.loadQuotationToEdit(updatedQuote!!)
        testScheduler.advanceUntilIdle()

        quotationViewModel.setDiscount(1000.0) // Apply discount during edit
        testScheduler.advanceUntilIdle()

        var editedId = 0
        quotationViewModel.saveQuotation { id ->
            editedId = id
        }
        testScheduler.advanceUntilIdle()

        assertEquals(savedId, editedId) // ID must match original, verifying update instead of duplicate insert!
        
        val editedQuote = repository.getQuotationByIdDirect(editedId)
        assertEquals(1000.0, editedQuote?.discount ?: 0.0, 0.001)

        // 3. Test duplicate quotation
        var duplicatedQuoteNum = ""
        historyViewModel.duplicateQuotation(savedId) { newNum ->
            duplicatedQuoteNum = newNum
        }
        testScheduler.advanceUntilIdle()

        assertNotNull(duplicatedQuoteNum)
        assertTrue(duplicatedQuoteNum.isNotEmpty())
        assertNotEquals(savedQuote!!.quotationNumber, duplicatedQuoteNum)

        // Verify the duplicated quote in database
        val allQuotes = repository.allQuotations.first()
        val duplicatedQuote = allQuotes.find { it.quotationNumber == duplicatedQuoteNum }
        assertNotNull(duplicatedQuote)
        assertNotEquals(savedId, duplicatedQuote?.id) // Must have a brand new primary key ID
        assertEquals("Draft", duplicatedQuote?.status) // Duplicates must reset status to Draft
        assertEquals("Rohan Kumar", duplicatedQuote?.customerName)
        assertEquals(15000.0, duplicatedQuote?.subtotal ?: 0.0, 0.001)

        subtotalJob.cancel()
        gstJob.cancel()
        grandTotalJob.cancel()
    }
}
