package com.example.data.snapshot

import com.example.data.AppDatabase
import com.example.data.QuotesRepository
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.models.FinalizedQuotationSnapshot

class QuotationSnapshotRepositoryImpl(
    private val db: AppDatabase,
    private val quotesRepository: QuotesRepository
) : QuotationSnapshotRepository {

    override suspend fun saveSnapshot(snapshot: FinalizedQuotationSnapshot) {
        val (quotation, items) = QuotationSnapshotMapper.toEntity(snapshot)
        quotesRepository.saveQuotationWithItems(quotation, items)
    }

    override suspend fun getSnapshotById(id: String): FinalizedQuotationSnapshot? {
        val quotationId = id.toIntOrNull() ?: return null
        val quotation = db.quotationDao().getQuotationByIdDirect(quotationId) ?: return null
        val items = db.quotationItemDao().getItemsForQuotationDirect(quotationId)
        return QuotationSnapshotMapper.toDomain(quotation, items)
    }

    override suspend fun getSnapshotByNumber(quotationNumber: String): FinalizedQuotationSnapshot? {
        val quotation = db.quotationDao().getQuotationByNumberDirect(quotationNumber) ?: return null
        val items = db.quotationItemDao().getItemsForQuotationDirect(quotation.id)
        return QuotationSnapshotMapper.toDomain(quotation, items)
    }

    override suspend fun getAllSnapshots(): List<FinalizedQuotationSnapshot> {
        val quotations = db.quotationDao().getAllQuotationsDirect()
        val allItems = db.quotationItemDao().getAllQuotationItemsDirect()
        val itemsByQuotationId = allItems.groupBy { it.quotationId }
        
        return quotations.map { quotation ->
            val items = itemsByQuotationId[quotation.id] ?: emptyList()
            QuotationSnapshotMapper.toDomain(quotation, items)
        }
    }
}
