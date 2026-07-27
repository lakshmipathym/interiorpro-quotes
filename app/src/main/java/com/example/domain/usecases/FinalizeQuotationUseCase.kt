package com.example.domain.usecases

import com.example.domain.contracts.QuotationSnapshotFactory
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.CompanySnapshot
import com.example.domain.models.CustomerSnapshot
import com.example.domain.models.FinalizedQuotationSnapshot
import com.example.domain.models.RawQuotationInput

class FinalizeQuotationUseCase(
    private val snapshotFactory: QuotationSnapshotFactory,
    private val snapshotRepository: QuotationSnapshotRepository
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
        val snapshot = snapshotFactory.createSnapshot(
            id = id,
            quotationNumber = quotationNumber,
            date = date,
            customer = customer,
            company = company,
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
