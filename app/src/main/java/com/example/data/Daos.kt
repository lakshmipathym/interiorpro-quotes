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
interface MasterDataDao {
    @Query("SELECT * FROM master_data ORDER BY value ASC")
    fun getAllMasterData(): Flow<List<MasterData>>

    @Query("SELECT * FROM master_data WHERE type = :type ORDER BY value ASC")
    fun getMasterDataByType(type: String): Flow<List<MasterData>>

    @Query("SELECT * FROM master_data WHERE type = :type ORDER BY value ASC")
    suspend fun getMasterDataByTypeDirect(type: String): List<MasterData>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMasterData(master: MasterData): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(masters: List<MasterData>)

    @Delete
    suspend fun deleteMasterData(master: MasterData)

    @Query("DELETE FROM master_data WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface QuotationTemplateDao {
    @Query("SELECT * FROM quotation_template ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<QuotationTemplate>>

    @Query("SELECT * FROM quotation_template")
    suspend fun getAllTemplatesDirect(): List<QuotationTemplate>

    @Query("SELECT * FROM quotation_template WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Int): QuotationTemplate?

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

@Dao
interface ClientDao {
    @Query("SELECT * FROM client ORDER BY createdDate DESC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM client WHERE isActive = 1 ORDER BY clientName ASC")
    fun getActiveClients(): Flow<List<Client>>

    @Query("SELECT * FROM client WHERE clientId = :id LIMIT 1")
    suspend fun getClientById(id: Long): Client?

    @Query("SELECT * FROM client WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getClientByMobile(mobile: String): Client?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client): Long

    @Update
    suspend fun updateClient(client: Client)

    @Delete
    suspend fun deleteClient(client: Client)

    @Query("UPDATE client SET isActive = :isActive, modifiedDate = :timestamp WHERE clientId = :id")
    suspend fun updateClientStatus(id: Long, isActive: Boolean, timestamp: Long = System.currentTimeMillis())
}

