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
        val normalizedType = when(masterType) {
            "PROJECT_CATEGORY" -> "CATEGORY"
            "MATERIAL_TYPE" -> "MATERIAL"
            else -> masterType
        }
        return try {
            when (normalizedType) {
                "PROJECT_TYPE" -> {
                    val usedInQuote = quotationDao.countByProjectType(trimmedName) > 0
                    val usedInTemplate = quotationTemplateDao.countByProjectType(trimmedName) > 0
                    usedInQuote || usedInTemplate
                }
                "CATEGORY" -> {
                    val usedInQuote = quotationDao.countByCategory(trimmedName) > 0
                    val usedInTemplate = quotationTemplateDao.countByCategory(trimmedName) > 0
                    usedInQuote || usedInTemplate
                }
                "MATERIAL" -> {
                    val usedInQuote = quotationDao.countByMaterial(trimmedName) > 0
                    val usedInTemplate = quotationTemplateDao.countByMaterial(trimmedName) > 0
                    usedInQuote || usedInTemplate
                }
                "FINISH_TYPE" -> {
                    val usedInQuote = quotationDao.countByFinish(trimmedName) > 0
                    val usedInTemplate = quotationTemplateDao.countByFinish(trimmedName) > 0
                    usedInQuote || usedInTemplate
                }
                "UNIT" -> {
                    quotationItemDao.countByUnit(trimmedName) > 0
                }
                "WARRANTY" -> {
                    quotationDao.countByWarranty(trimmedName) > 0
                }
                "TERMS" -> {
                    quotationDao.countByTerms(trimmedName) > 0
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}
