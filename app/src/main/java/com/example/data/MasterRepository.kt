package com.example.data

import kotlinx.coroutines.flow.Flow

class MasterRepository(private val db: AppDatabase) {
    private val masterDao = db.masterDao()
    private val quotationDao = db.quotationDao()
    private val quotationTemplateDao = db.quotationTemplateDao()
    private val quotationItemDao = db.quotationItemDao()

    fun getAllMasters(): Flow<List<MasterEntity>> = masterDao.getAllMastersFlow()

    fun getMastersByType(type: String): Flow<List<MasterEntity>> = masterDao.getMastersByTypeFlow(type)

    suspend fun getMastersByTypeDirect(type: String): List<MasterEntity> = masterDao.getMastersByTypeDirect(type)

    suspend fun getMasterById(id: Long): MasterEntity? = masterDao.getMasterById(id)

    suspend fun getMasterByTypeAndName(type: String, name: String): MasterEntity? = masterDao.getMasterByTypeAndName(type, name)

    suspend fun saveMaster(master: MasterEntity): Long {
        return masterDao.insertMaster(master)
    }

    suspend fun updateMaster(master: MasterEntity) {
        masterDao.updateMaster(master)
    }

    suspend fun softDeleteMaster(id: Long) {
        masterDao.softDeleteMaster(id)
    }

    suspend fun deleteMasterPermanently(master: MasterEntity) {
        masterDao.deleteMasterPermanently(master)
    }

    suspend fun isMasterUsed(masterType: String, name: String): Boolean {
        val trimmedName = name.trim()
        return try {
            when (masterType) {
                "PROJECT_TYPE" -> {
                    val usedInQuote = quotationDao.getAllQuotationsDirect().any { it.projectType.trim().equals(trimmedName, ignoreCase = true) }
                    val usedInTemplate = quotationTemplateDao.getAllTemplatesDirect().any { it.projectType.trim().equals(trimmedName, ignoreCase = true) }
                    usedInQuote || usedInTemplate
                }
                "CATEGORY" -> {
                    val usedInQuote = quotationDao.getAllQuotationsDirect().any { it.category.trim().equals(trimmedName, ignoreCase = true) }
                    val usedInTemplate = quotationTemplateDao.getAllTemplatesDirect().any { it.category.trim().equals(trimmedName, ignoreCase = true) }
                    usedInQuote || usedInTemplate
                }
                "MATERIAL" -> {
                    val usedInQuote = quotationDao.getAllQuotationsDirect().any { it.material.trim().equals(trimmedName, ignoreCase = true) }
                    val usedInTemplate = quotationTemplateDao.getAllTemplatesDirect().any { it.material.trim().equals(trimmedName, ignoreCase = true) }
                    usedInQuote || usedInTemplate
                }
                "FINISH_TYPE" -> {
                    val usedInQuote = quotationDao.getAllQuotationsDirect().any { it.finish.trim().equals(trimmedName, ignoreCase = true) }
                    val usedInTemplate = quotationTemplateDao.getAllTemplatesDirect().any { it.finish.trim().equals(trimmedName, ignoreCase = true) }
                    usedInQuote || usedInTemplate
                }
                "UNIT" -> {
                    val usedInItems = quotationItemDao.getAllQuotationItemsDirect().any { it.unit.trim().equals(trimmedName, ignoreCase = true) }
                    usedInItems
                }
                "WARRANTY" -> {
                    quotationDao.getAllQuotationsDirect().any { it.warranty.trim().equals(trimmedName, ignoreCase = true) }
                }
                "TERMS" -> {
                    quotationDao.getAllQuotationsDirect().any { it.termsAndConditions.contains(trimmedName, ignoreCase = true) }
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}
