package com.example.history

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Quotation
import com.example.data.QuotesRepository
import com.example.ui.quotation.QuotationViewModel
import com.example.domain.contracts.QuotationSnapshotFactory
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.engine.QuotationSnapshotFactoryImpl
import com.example.domain.usecases.CalculateQuotationUseCase
import com.example.domain.usecases.FinalizeQuotationUseCase
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.engine.DimensionParserImpl
import com.example.domain.engine.ItemCalculationEngineImpl
import com.example.domain.engine.QuotationCalculationEngineImpl
import com.example.domain.engine.AmountInWordsConverterImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoricalQuotationIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: QuotesRepository
    private lateinit var viewModel: QuotationViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        db = androidx.room.Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = QuotesRepository(db)
        
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = CalculateQuotationUseCase(itemEngine, calcEngine)
        val snapshotRepository = QuotationSnapshotRepositoryImpl(db, repository)
        val finalizeUseCase = FinalizeQuotationUseCase(
            QuotationSnapshotFactoryImpl(),
            snapshotRepository
        )
        val masterRepository = com.example.data.MasterRepository(db)
        val syncManager = object : com.example.core.sync.SyncManager {
            override val syncState = kotlinx.coroutines.flow.MutableStateFlow(com.example.core.sync.SyncState.Idle)
            override suspend fun triggerSync(): com.example.core.sync.SyncResult = com.example.core.sync.SyncResult.Success(0)
            override suspend fun resolveConflicts(preferCloud: Boolean): com.example.core.sync.SyncResult = com.example.core.sync.SyncResult.Success(0)
            override suspend fun clearSyncState() {}
            override fun onQuotationSaved() {}
            override suspend fun checkForNewerBackup(): com.example.core.backup.BackupMetadata? = null
            override fun getAutoBackupPolicy(): String = "MANUAL"
            override fun setAutoBackupPolicy(policy: String) {}
        }
        
        viewModel = QuotationViewModel(application, repository, masterRepository, syncManager, calcUseCase, finalizeUseCase, snapshotRepository)
    }

    @Test
    fun testEditFinalizedQuotationCreatesNewDraft() = runBlocking {
        val originalQuote = Quotation(
            quotationNumber = "IPQ/TEST/001",
            status = "Final",
            customerName = "Customer A"
        )
        val id = db.quotationDao().insertQuotation(originalQuote)
        val loadedQuote = db.quotationDao().getQuotationByIdDirect(id.toInt())!!

        viewModel.loadQuotationToEdit(loadedQuote)
        
        kotlinx.coroutines.delay(100)

        assertEquals("Draft", viewModel.editingQuotationStatus.value)
        assertEquals(0, viewModel.editingQuotationId.value)
        assertEquals("", viewModel.newQuoteNumber.value)
    }

    @Test
    fun testEditDraftQuotationRetainsOriginalId() = runBlocking {
        val originalQuote = Quotation(
            quotationNumber = "IPQ/TEST/002",
            status = "Draft",
            customerName = "Customer B"
        )
        val id = db.quotationDao().insertQuotation(originalQuote)
        val loadedQuote = db.quotationDao().getQuotationByIdDirect(id.toInt())!!

        viewModel.loadQuotationToEdit(loadedQuote)
        
        kotlinx.coroutines.delay(100)

        assertEquals("Draft", viewModel.editingQuotationStatus.value)
        assertEquals(id.toInt(), viewModel.editingQuotationId.value)
        assertEquals("IPQ/TEST/002", viewModel.newQuoteNumber.value)
    }
}
