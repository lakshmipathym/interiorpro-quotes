cat << 'INNER_EOF' > app/src/main/java/com/example/domain/usecases/FinalizeQuotationUseCase.kt
package com.example.domain.usecases

import com.example.domain.contracts.BrandingAssetCopier
import com.example.domain.contracts.QuotationSnapshotFactory
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.CompanySnapshot
import com.example.domain.models.CustomerSnapshot
import com.example.domain.models.FinalizedQuotationSnapshot
import com.example.domain.models.RawQuotationInput

class FinalizeQuotationUseCase(
    private val snapshotFactory: QuotationSnapshotFactory,
    private val snapshotRepository: QuotationSnapshotRepository,
    private val assetCopier: BrandingAssetCopier? = null
) {
    suspend fun execute(
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
        val updatedCompany = assetCopier?.copyAssetsForQuotation(quotationNumber, company) ?: company

        val snapshot = snapshotFactory.createSnapshot(
            id = id,
            quotationNumber = quotationNumber,
            date = date,
            customer = customer,
            company = updatedCompany,
            termsAndConditions = termsAndConditions,
            warranty = warranty,
            validityDays = validityDays,
            notes = notes,
            rawInput = rawInput,
            calculatedQuotation = calculatedQuotation
        )
        snapshotRepository.saveSnapshot(snapshot)
        return snapshot
    }
}
INNER_EOF
