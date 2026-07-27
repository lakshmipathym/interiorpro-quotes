package com.example.core.sync.validation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BackupValidationRepositoryImpl : BackupValidationRepository {
    private val _validationReports = MutableStateFlow<List<ValidationReport>>(emptyList())
    private val _integrityReports = MutableStateFlow<List<IntegrityReport>>(emptyList())

    override fun observeRecentValidationReports(): Flow<List<ValidationReport>> = _validationReports.asStateFlow()

    override fun observeRecentIntegrityReports(): Flow<List<IntegrityReport>> = _integrityReports.asStateFlow()

    override suspend fun saveValidationReport(report: ValidationReport) {
        _validationReports.update { current ->
            (listOf(report) + current).take(10)
        }
    }

    override suspend fun saveIntegrityReport(report: IntegrityReport) {
        _integrityReports.update { current ->
            (listOf(report) + current).take(10)
        }
    }
}
