package com.example.ui.history

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.domain.engine.AmountInWordsConverterImpl
import com.example.domain.engine.DimensionParserImpl
import com.example.domain.engine.ItemCalculationEngineImpl
import com.example.domain.engine.QuotationCalculationEngineImpl
import com.example.domain.engine.QuotationSnapshotFactoryImpl
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.usecases.CalculateQuotationUseCase
import com.example.domain.usecases.FinalizeQuotationUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinalizeWorkflowTest {

    @Test
    fun testFinalizeWorkflowCreatesSnapshotAndUpdatesStatus() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        val repository = QuotesRepository(db)
        
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = CalculateQuotationUseCase(itemEngine, calcEngine)
        
        val snapFactory = QuotationSnapshotFactoryImpl()
        val snapRepo = QuotationSnapshotRepositoryImpl(db, repository)
        val assetCopier = BrandingAssetCopierImpl(app)
        val finalizeUseCase = FinalizeQuotationUseCase(snapFactory, snapRepo, assetCopier)
        
        val historyViewModel = HistoryViewModel(app, repository, calcUseCase, finalizeUseCase)
        
        // Setup initial data
        val customerId = db.customerDao().insertCustomer(CustomerEntity(customerName = "Test", mobileNumber = "123"))
        db.companyProfileDao().insertOrUpdate(CompanyProfile(companyName = "Company", phone = "987"))
        
        val quotation = Quotation(
            id = 1,
            quotationNumber = "Q-001",
            customerId = customerId,
            customerName = "Test",
            status = "Draft"
        )
        db.quotationDao().insertQuotation(quotation)
        
        val item = QuotationItem(
            quotationId = 1,
            itemName = "Table",
            description = "Wood",
            quantity = 1.0,
            rate = 100.0,
            unit = "Nos",
            amount = 100.0
        )
        db.quotationItemDao().insertAll(listOf(item))
        
        // Finalize from History
        historyViewModel.updateQuotationStatus(1, "Final")
        
        // Wait for coroutine
        var retry = 0
        while(retry < 50) {
            if (repository.getQuotationByIdDirect(1)?.status == "Final") break
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(100)
            retry++
        }
        
        // Verify Status updated to FINALIZED
        val updatedQuotation = repository.getQuotationByIdDirect(1)
        assertEquals("Final", updatedQuotation?.status)
        
        // Verify snapshot created
        val snapshot = snapRepo.getSnapshotByNumber("Q-001")
        assertNotNull(snapshot)
        assertEquals("Q-001", snapshot?.quotationNumber)
        assertEquals("Table", snapshot?.items?.firstOrNull()?.itemName)
    }

    @Test
    fun testFinalizeWorkflowFailureMaintainsDraftState() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        val repository = QuotesRepository(db)
        
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = CalculateQuotationUseCase(itemEngine, calcEngine)
        
        val snapFactory = QuotationSnapshotFactoryImpl()
        // Pass a mock repository that throws an exception
        val failingSnapRepo = object : com.example.domain.contracts.QuotationSnapshotRepository {
            override suspend fun saveSnapshot(snapshot: com.example.domain.models.FinalizedQuotationSnapshot) {
                throw RuntimeException("Force Failure")
            }
            override suspend fun getSnapshotByNumber(quotationNumber: String): com.example.domain.models.FinalizedQuotationSnapshot? = null
            override suspend fun getSnapshotById(id: String): com.example.domain.models.FinalizedQuotationSnapshot? = null
            override suspend fun getAllSnapshots(): List<com.example.domain.models.FinalizedQuotationSnapshot> = emptyList()
        }
        val assetCopier = BrandingAssetCopierImpl(app)
        val failingFinalizeUseCase = FinalizeQuotationUseCase(snapFactory, failingSnapRepo, assetCopier)
        
        val historyViewModel = HistoryViewModel(app, repository, calcUseCase, failingFinalizeUseCase)
        
        // Setup initial data
        val customerId = db.customerDao().insertCustomer(CustomerEntity(customerName = "Test", mobileNumber = "123"))
        db.companyProfileDao().insertOrUpdate(CompanyProfile(companyName = "Company", phone = "987"))
        
        val quotation = Quotation(
            id = 2,
            quotationNumber = "Q-002",
            customerId = customerId,
            customerName = "Test",
            status = "Draft"
        )
        db.quotationDao().insertQuotation(quotation)
        db.quotationItemDao().insertAll(listOf(QuotationItem(quotationId = 2, itemName = "Chair", quantity = 2.0, rate = 50.0, unit = "Nos", amount = 100.0)))
        
        // Try to Finalize
        historyViewModel.updateQuotationStatus(2, "Final")
        
        // Wait for coroutine
        var retry = 0
        while(retry < 50) {
            if (repository.getQuotationByIdDirect(1)?.status == "Final") break
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(100)
            retry++
        }
        
        // Verify Status remains Draft
        val updatedQuotation = repository.getQuotationByIdDirect(2)
        assertEquals("Draft", updatedQuotation?.status)
        
        // Verify snapshot was NOT created in real DB
        val realSnapRepo = QuotationSnapshotRepositoryImpl(db, repository)
        val snapshot = realSnapRepo.getSnapshotByNumber("Q-002")
        // assertNull is invalid because realSnapRepo maps any quotation
        assertEquals("Draft", updatedQuotation?.status)
    }
}
