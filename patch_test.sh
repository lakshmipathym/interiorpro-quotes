cat << 'INNER_EOF' > app/src/test/java/com/example/domain/usecases/FinalizeQuotationUseCaseTest.kt
package com.example.domain.usecases

import com.example.domain.contracts.BrandingAssetCopier
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
    
    private class DummyBrandingAssetCopier : BrandingAssetCopier {
        var copyAssetsCalled = false
        override suspend fun copyAssetsForQuotation(quotationNumber: String, company: CompanySnapshot): CompanySnapshot {
            copyAssetsCalled = true
            return company.copy(
                logoPath = "/quotation_assets/${quotationNumber}/logo.png",
                signaturePath = "/quotation_assets/${quotationNumber}/signature.png",
                companySealPath = "/quotation_assets/${quotationNumber}/seal.png"
            )
        }
    }

    private lateinit var snapshotFactory: DummyQuotationSnapshotFactory
    private lateinit var snapshotRepository: DummyQuotationSnapshotRepository
    private lateinit var assetCopier: DummyBrandingAssetCopier
    private lateinit var finalizeQuotationUseCase: FinalizeQuotationUseCase

    @Before
    fun setup() {
        snapshotFactory = DummyQuotationSnapshotFactory()
        snapshotRepository = DummyQuotationSnapshotRepository()
        assetCopier = DummyBrandingAssetCopier()
        finalizeQuotationUseCase = FinalizeQuotationUseCase(snapshotFactory, snapshotRepository, assetCopier)
    }

    @Test
    fun `test asset copying updates snapshot paths`() = runBlocking {
        val calculatedQuotation = CalculatedQuotation(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, "")
        
        val company = CompanySnapshot(
            companyName = "Test Co",
            ownerName = "Test Owner",
            phone = "123",
            email = "test@co",
            address = "Test Addr",
            bankName = "Test Bank",
            accountHolderName = "Test Acc",
            accountNumber = "123",
            ifsc = "IFSC",
            branch = "Branch",
            upiId = "UPI",
            logoPath = "/original/logo.png",
            signaturePath = "/original/sig.png",
            companySealPath = "/original/seal.png"
        )
        
        val result = finalizeQuotationUseCase.execute(
            id = "1",
            quotationNumber = "Q-1",
            date = 0L,
            customer = CustomerSnapshot("0", "", "", "", "", ""),
            company = company,
            termsAndConditions = "",
            warranty = "",
            validityDays = 0,
            notes = "",
            rawInput = RawQuotationInput(),
            calculatedQuotation = calculatedQuotation
        )

        assertTrue(assetCopier.copyAssetsCalled)
        assertEquals("/quotation_assets/Q-1/logo.png", result.company.logoPath)
        assertEquals("/quotation_assets/Q-1/signature.png", result.company.signaturePath)
        assertEquals("/quotation_assets/Q-1/seal.png", result.company.companySealPath)
    }
}
INNER_EOF
