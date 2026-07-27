package com.example.domain.usecases

import com.example.domain.contracts.QuotationSnapshotFactory
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FinalizeQuotationUseCaseTest {

    private class DummyQuotationSnapshotFactory : QuotationSnapshotFactory {
        var createSnapshotCalled = false
        override fun createSnapshot(
            id: String,
            quotationNumber: String,
            date: Long,
            customer: CustomerSnapshot,
            company: CompanySnapshot,
            termsAndConditions: String,
            warranty: String,
            validityDays: Int,
            notes: String,
            rawInput: RawQuotationInput,
            calculatedQuotation: CalculatedQuotation
        ): FinalizedQuotationSnapshot {
            createSnapshotCalled = true
            return FinalizedQuotationSnapshot(
                id = id,
                quotationNumber = quotationNumber,
                date = date,
                customer = customer,
                company = company,
                items = emptyList(),
                financial = FinancialSnapshot(
                    subtotal = calculatedQuotation.subtotal,
                    discount = rawInput.discount,
                    taxableAmount = calculatedQuotation.taxableAmount,
                    gstRate = rawInput.gstRate,
                    gstAmount = calculatedQuotation.gstAmount,
                    transport = rawInput.transport,
                    installation = rawInput.installation,
                    extraCharges = rawInput.extraCharges,
                    roundOff = rawInput.roundOff,
                    grandTotal = calculatedQuotation.grandTotal,
                    advance = rawInput.advance,
                    balanceDue = calculatedQuotation.balanceDue,
                    amountInWords = calculatedQuotation.amountInWords
                ),
                termsAndConditions = termsAndConditions,
                warranty = warranty,
                validityDays = validityDays,
                notes = notes
            )
        }
    }

    private class DummyQuotationSnapshotRepository : QuotationSnapshotRepository {
        var saveSnapshotCalled = false
        override suspend fun saveSnapshot(snapshot: FinalizedQuotationSnapshot) {
            saveSnapshotCalled = true
        }

        override suspend fun getSnapshotById(id: String): FinalizedQuotationSnapshot? = null
        override suspend fun getSnapshotByNumber(quotationNumber: String): FinalizedQuotationSnapshot? = null
        override suspend fun getAllSnapshots(): List<FinalizedQuotationSnapshot> = emptyList()
    }

    private lateinit var snapshotFactory: DummyQuotationSnapshotFactory
    private lateinit var snapshotRepository: DummyQuotationSnapshotRepository
    private lateinit var finalizeQuotationUseCase: FinalizeQuotationUseCase

    @Before
    fun setup() {
        snapshotFactory = DummyQuotationSnapshotFactory()
        snapshotRepository = DummyQuotationSnapshotRepository()
        finalizeQuotationUseCase = FinalizeQuotationUseCase(snapshotFactory, snapshotRepository)
    }

    @Test
    fun `test valid CalculatedQuotation triggers Snapshot Factory`() = runBlocking {
        val calculatedQuotation = CalculatedQuotation(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, "")
        
        finalizeQuotationUseCase.execute(
            id = "1",
            quotationNumber = "Q-1",
            date = 0L,
            customer = CustomerSnapshot("0", "", "", "", "", ""),
            company = CompanySnapshot("", "", "", "", "", "", "", "", "", "", "", ""),
            termsAndConditions = "",
            warranty = "",
            validityDays = 0,
            notes = "",
            rawInput = RawQuotationInput(),
            calculatedQuotation = calculatedQuotation
        )

        assertTrue(snapshotFactory.createSnapshotCalled)
    }

    @Test
    fun `test snapshot creation returns FinalizedQuotationSnapshot successfully`() = runBlocking {
        val calculatedQuotation = CalculatedQuotation(emptyList(), 100.0, 100.0, 0.0, 100.0, 100.0, "")
        
        val result = finalizeQuotationUseCase.execute(
            id = "2",
            quotationNumber = "Q-2",
            date = 0L,
            customer = CustomerSnapshot("0", "", "", "", "", ""),
            company = CompanySnapshot("", "", "", "", "", "", "", "", "", "", "", ""),
            termsAndConditions = "",
            warranty = "",
            validityDays = 0,
            notes = "",
            rawInput = RawQuotationInput(),
            calculatedQuotation = calculatedQuotation
        )

        assertEquals("2", result.id)
        assertEquals("Q-2", result.quotationNumber)
        assertEquals(100.0, result.financial.subtotal, 0.0)
    }

    @Test
    fun `test persistence repository receives the finalized snapshot`() = runBlocking {
        val calculatedQuotation = CalculatedQuotation(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, "")
        
        finalizeQuotationUseCase.execute(
            id = "3",
            quotationNumber = "Q-3",
            date = 0L,
            customer = CustomerSnapshot("0", "", "", "", "", ""),
            company = CompanySnapshot("", "", "", "", "", "", "", "", "", "", "", ""),
            termsAndConditions = "",
            warranty = "",
            validityDays = 0,
            notes = "",
            rawInput = RawQuotationInput(),
            calculatedQuotation = calculatedQuotation
        )

        assertTrue(snapshotRepository.saveSnapshotCalled)
    }

    @Test
    fun `test exact preservation of financial values from CalculatedQuotation to snapshot`() = runBlocking {
        val calculatedQuotation = CalculatedQuotation(
            items = emptyList(),
            subtotal = 1500.0,
            taxableAmount = 1400.0,
            gstAmount = 252.0,
            grandTotal = 1822.5,
            balanceDue = 1322.5,
            amountInWords = "Test"
        )
        val rawInput = RawQuotationInput(
            discount = 100.0,
            gstRate = 18.0,
            transport = 50.0,
            installation = 100.0,
            extraCharges = 20.0,
            roundOff = 0.5,
            advance = 500.0
        )
        
        val result = finalizeQuotationUseCase.execute(
            id = "4",
            quotationNumber = "Q-4",
            date = 0L,
            customer = CustomerSnapshot("0", "", "", "", "", ""),
            company = CompanySnapshot("", "", "", "", "", "", "", "", "", "", "", ""),
            termsAndConditions = "",
            warranty = "",
            validityDays = 0,
            notes = "",
            rawInput = rawInput,
            calculatedQuotation = calculatedQuotation
        )

        assertEquals(1500.0, result.financial.subtotal, 0.0)
        assertEquals(100.0, result.financial.discount, 0.0)
        assertEquals(1400.0, result.financial.taxableAmount, 0.0)
        assertEquals(18.0, result.financial.gstRate, 0.0)
        assertEquals(252.0, result.financial.gstAmount, 0.0)
        assertEquals(50.0, result.financial.transport, 0.0)
        assertEquals(100.0, result.financial.installation, 0.0)
        assertEquals(20.0, result.financial.extraCharges, 0.0)
        assertEquals(0.5, result.financial.roundOff, 0.0)
        assertEquals(1822.5, result.financial.grandTotal, 0.0)
        assertEquals(500.0, result.financial.advance, 0.0)
        assertEquals(1322.5, result.financial.balanceDue, 0.0)
        assertEquals("Test", result.financial.amountInWords)
    }
}
