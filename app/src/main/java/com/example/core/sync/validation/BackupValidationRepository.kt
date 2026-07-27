package com.example.core.sync.validation

import kotlinx.coroutines.flow.Flow

interface BackupValidationRepository {
    fun observeRecentValidationReports(): Flow<List<ValidationReport>>
    fun observeRecentIntegrityReports(): Flow<List<IntegrityReport>>
    
    suspend fun saveValidationReport(report: ValidationReport)
    suspend fun saveIntegrityReport(report: IntegrityReport)
}
