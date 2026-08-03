package com.example.domain.contracts

import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.CompanySnapshot
import com.example.domain.models.CustomerSnapshot
import com.example.domain.models.FinalizedQuotationSnapshot
import com.example.domain.models.RawQuotationInput

interface QuotationSnapshotFactory {
    fun createSnapshot(
        id: String,
        quotationNumber: String,
        date: Long,
        customer: CustomerSnapshot,
        company: CompanySnapshot,
        termsAndConditions: String,
        warranty: String,
        deliveryTime: String,
        installationTime: String,
        paymentTerms: String,
        additionalConditions: String,
        validityDays: Int,
        notes: String,
        rawInput: RawQuotationInput,
        calculatedQuotation: CalculatedQuotation
    ): FinalizedQuotationSnapshot
}

interface QuotationSnapshotRepository {
    suspend fun saveSnapshot(snapshot: FinalizedQuotationSnapshot)
    suspend fun getSnapshotById(id: String): FinalizedQuotationSnapshot?
    suspend fun getSnapshotByNumber(quotationNumber: String): FinalizedQuotationSnapshot?
    suspend fun getAllSnapshots(): List<FinalizedQuotationSnapshot>
}
