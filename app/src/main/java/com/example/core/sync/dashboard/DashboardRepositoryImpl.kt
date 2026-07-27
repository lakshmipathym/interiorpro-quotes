package com.example.core.sync.dashboard

import com.example.data.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DashboardRepositoryImpl(private val db: AppDatabase) : DashboardRepository {

    override fun getSyncSummary(): Flow<SyncSummary> {
        val quotationCount = db.quotationDao().getAllQuotations().map { it.size }
        val customerCount = db.customerDao().getAllCustomers().map { it.size }
        val masterCount = db.masterDao().getAllMastersFlow().map { it.size }
        
        return combine(quotationCount, customerCount, masterCount) { qc, cc, mc ->
            SyncSummary(
                syncStatus = SyncStatus.IDLE,
                lastBackupTime = null,
                lastBackupStatus = "Never",
                lastBackupSize = 0L,
                connectedAccount = null,
                isAccountConnected = false,
                customerCount = cc,
                quotationCount = qc,
                masterRecordsCount = mc,
                databaseVersion = 1
            )
        }
    }

    override fun getWorkspaceHealth(): Flow<WorkspaceHealth> {
        return flowOf(
            WorkspaceHealth(
                isBackupReady = false,
                isRestoreReady = false,
                isSyncReady = false,
                isPdfEngineReady = true,
                isDatabaseHealthy = true
            )
        )
    }
}
