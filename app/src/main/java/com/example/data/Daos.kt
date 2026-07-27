package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<CompanyProfile?>

    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileDirect(): CompanyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: CompanyProfile)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customer ORDER BY createdDate DESC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customer WHERE customerId = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customer WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getCustomerByMobile(mobile: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)
}

@Dao
interface QuotationTemplateDao {
    @Query("SELECT * FROM quotation_template ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<QuotationTemplate>>

    @Query("SELECT * FROM quotation_template")
    suspend fun getAllTemplatesDirect(): List<QuotationTemplate>

    @Query("SELECT * FROM quotation_template WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Int): QuotationTemplate?

    @Query("SELECT COUNT(id) FROM quotation_template WHERE LOWER(TRIM(projectType)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByProjectType(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation_template WHERE LOWER(TRIM(category)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByCategory(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation_template WHERE LOWER(TRIM(material)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByMaterial(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation_template WHERE LOWER(TRIM(finish)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByFinish(name: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: QuotationTemplate): Long

    @Delete
    suspend fun deleteTemplate(template: QuotationTemplate)
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotation ORDER BY date DESC")
    fun getAllQuotations(): Flow<List<Quotation>>

    @Query("SELECT * FROM quotation")
    suspend fun getAllQuotationsDirect(): List<Quotation>

    @Query("SELECT * FROM quotation WHERE id = :id LIMIT 1")
    fun getQuotationById(id: Int): Flow<Quotation?>

    @Query("SELECT * FROM quotation WHERE id = :id LIMIT 1")
    suspend fun getQuotationByIdDirect(id: Int): Quotation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: Quotation): Long

    @Update
    suspend fun updateQuotation(quotation: Quotation)

    @Delete
    suspend fun deleteQuotation(quotation: Quotation)

    @Query("DELETE FROM quotation WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(id) FROM quotation WHERE LOWER(TRIM(projectType)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByProjectType(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation WHERE LOWER(TRIM(category)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByCategory(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation WHERE LOWER(TRIM(material)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByMaterial(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation WHERE LOWER(TRIM(finish)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByFinish(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation WHERE LOWER(TRIM(warranty)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByWarranty(name: String): Int

    @Query("SELECT COUNT(id) FROM quotation WHERE LOWER(termsAndConditions) LIKE '%' || LOWER(TRIM(:name)) || '%' LIMIT 1")
    suspend fun countByTerms(name: String): Int

    @Query("SELECT * FROM quotation WHERE quotationNumber = :quotationNumber LIMIT 1")
    suspend fun getQuotationByNumberDirect(quotationNumber: String): Quotation?

    @Query("SELECT quotationNumber FROM quotation WHERE quotationNumber LIKE :prefix || '%' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestQuotationNumber(prefix: String): String?
}

@Dao
interface QuotationItemDao {
    @Query("SELECT * FROM quotation_item WHERE quotationId = :quotationId ORDER BY id ASC")
    fun getItemsForQuotation(quotationId: Int): Flow<List<QuotationItem>>

    @Query("SELECT * FROM quotation_item WHERE quotationId = :quotationId ORDER BY id ASC")
    suspend fun getItemsForQuotationDirect(quotationId: Int): List<QuotationItem>

    @Query("SELECT * FROM quotation_item")
    suspend fun getAllQuotationItemsDirect(): List<QuotationItem>

    @Query("SELECT COUNT(id) FROM quotation_item WHERE LOWER(TRIM(unit)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun countByUnit(name: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QuotationItem>)

    @Query("DELETE FROM quotation_item WHERE quotationId = :quotationId")
    suspend fun deleteForQuotation(quotationId: Int)
}

@Dao
interface MasterDao {
    @Query("SELECT * FROM masters")
    suspend fun getAllMastersDirect(): List<MasterEntity>

    @Query("SELECT * FROM masters WHERE isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getAllMastersFlow(): Flow<List<MasterEntity>>

    @Query("SELECT * FROM masters WHERE masterType = :type AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getMastersByTypeFlow(type: String): Flow<List<MasterEntity>>

    @Query("SELECT * FROM masters WHERE masterType = :type AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getMastersByTypeDirect(type: String): List<MasterEntity>

    @Query("SELECT * FROM masters WHERE id = :id LIMIT 1")
    suspend fun getMasterById(id: Long): MasterEntity?

    @Query("SELECT * FROM masters WHERE masterType = :type AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND isDeleted = 0 LIMIT 1")
    suspend fun getMasterByTypeAndName(type: String, name: String): MasterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaster(master: MasterEntity): Long

    @Update
    suspend fun updateMaster(master: MasterEntity)

    @Delete
    suspend fun deleteMasterPermanently(master: MasterEntity)

    // Soft Delete
    @Query("UPDATE masters SET isDeleted = 1, modifiedDate = :timestamp WHERE id = :id")
    suspend fun softDeleteMaster(id: Long, timestamp: Long = System.currentTimeMillis())
}


