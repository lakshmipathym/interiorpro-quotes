package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class QuotesRepository(private val db: AppDatabase) {

    private val companyProfileDao = db.companyProfileDao()
    private val customerDao = db.customerDao()
    private val quotationTemplateDao = db.quotationTemplateDao()
    private val quotationDao = db.quotationDao()
    private val quotationItemDao = db.quotationItemDao()

    // --- COMPANY PROFILE ---
    val companyProfile: Flow<CompanyProfile?> = companyProfileDao.getProfile()
    suspend fun getCompanyProfileDirect(): CompanyProfile? = companyProfileDao.getProfileDirect()
    suspend fun saveCompanyProfile(profile: CompanyProfile) = companyProfileDao.insertOrUpdate(profile)

    // --- CUSTOMERS ---
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    suspend fun getCustomerById(id: Long): CustomerEntity? = customerDao.getCustomerById(id)
    suspend fun getCustomerByMobile(mobile: String): CustomerEntity? = customerDao.getCustomerByMobile(mobile)
    suspend fun saveCustomer(customer: CustomerEntity): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: CustomerEntity) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: CustomerEntity) = customerDao.deleteCustomer(customer)

    // --- QUOTATION TEMPLATES ---
    val allTemplates: Flow<List<QuotationTemplate>> = quotationTemplateDao.getAllTemplates()
    suspend fun getTemplateById(id: Int): QuotationTemplate? = quotationTemplateDao.getTemplateById(id)
    suspend fun saveTemplate(template: QuotationTemplate): Long = quotationTemplateDao.insertTemplate(template)
    suspend fun deleteTemplate(template: QuotationTemplate) = quotationTemplateDao.deleteTemplate(template)

    // --- QUOTATIONS ---
    val allQuotations: Flow<List<Quotation>> = quotationDao.getAllQuotations()
    fun getQuotationById(id: Int): Flow<Quotation?> = quotationDao.getQuotationById(id)
    suspend fun getQuotationByIdDirect(id: Int): Quotation? = quotationDao.getQuotationByIdDirect(id)

    fun getQuotationItems(quotationId: Int): Flow<List<QuotationItem>> = quotationItemDao.getItemsForQuotation(quotationId)
    suspend fun getQuotationItemsDirect(quotationId: Int): List<QuotationItem> = quotationItemDao.getItemsForQuotationDirect(quotationId)

    // Transactional save for quotation + items
    suspend fun saveQuotationWithItems(quotation: Quotation, items: List<QuotationItem>): Int {
        return db.withTransaction {
            val qId = quotationDao.insertQuotation(quotation).toInt()
            // Delete old items if updating
            if (quotation.id > 0) {
                quotationItemDao.deleteForQuotation(quotation.id)
            }
            // Map items with the correct quotationId
            val finalItems = items.map { it.copy(id = 0, quotationId = if (quotation.id > 0) quotation.id else qId) }
            quotationItemDao.insertAll(finalItems)
            if (quotation.id > 0) quotation.id else qId
        }
    }

    suspend fun deleteQuotation(id: Int) {
        db.withTransaction {
            quotationItemDao.deleteForQuotation(id)
            quotationDao.deleteById(id)
        }
    }

    // Generate auto-incremented Quotation Number: e.g. IPQ/2026/0001
    suspend fun generateNextQuotationNumber(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val prefix = "IPQ/$year/"
        val latestNum = quotationDao.getLatestQuotationNumber(prefix)
        
        return if (latestNum != null && latestNum.startsWith(prefix)) {
            val suffix = latestNum.removePrefix(prefix)
            val number = suffix.toIntOrNull() ?: 0
            val nextNumber = number + 1
            String.format("%s%04d", prefix, nextNumber)
        } else {
            "${prefix}0001"
        }
    }
}
