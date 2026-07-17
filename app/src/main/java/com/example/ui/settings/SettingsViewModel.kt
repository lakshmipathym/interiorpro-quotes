package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.core.backup.BackupMetadata
import com.example.core.backup.WorkspaceManager
import com.example.core.backup.WorkspacePreview
import com.example.core.device.DeviceManager
import com.example.core.drive.GoogleSignInManager
import com.example.core.drive.DriveFileInfo
import com.example.core.sync.SyncManager
import com.example.core.sync.SyncResult
import com.example.core.sync.SyncState
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    application: Application,
    private val repository: QuotesRepository,
    val syncManager: SyncManager,
    val workspaceManager: WorkspaceManager,
    val deviceManager: DeviceManager,
    val signInManager: GoogleSignInManager
) : AndroidViewModel(application) {

    val allTemplates: StateFlow<List<QuotationTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val isUserSignedIn: StateFlow<Boolean> = signInManager.isUserSignedIn
    val currentUserEmail: StateFlow<String?> = signInManager.currentUserEmail
    val currentUserDisplayName: StateFlow<String?> = signInManager.currentUserDisplayName

    fun saveTemplate(template: QuotationTemplate) {
        viewModelScope.launch {
            repository.saveTemplate(template)
        }
    }

    fun deleteTemplate(template: QuotationTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun exportBackupData(password: String = "", onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val result = BackupManager.exportBackup(db, repository, password)
            onComplete(result)
        }
    }

    fun validateBackupData(json: String, password: String = ""): Boolean {
        return BackupManager.validateBackup(json, password)
    }

    fun importBackupData(json: String, password: String = "", onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val success = BackupManager.importBackup(db, repository, json, password)
            onComplete(success)
        }
    }

    // --- Sprint 3 Sync & Workspace triggers ---

    fun triggerSync(onResult: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = syncManager.triggerSync()
            onResult(result)
        }
    }

    fun resolveConflicts(preferCloud: Boolean, onResult: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            val result = syncManager.resolveConflicts(preferCloud)
            onResult(result)
        }
    }

    fun clearSyncStatus() {
        viewModelScope.launch {
            syncManager.clearSyncState()
        }
    }

    fun getAutoBackupPolicy(): String {
        return syncManager.getAutoBackupPolicy()
    }

    fun setAutoBackupPolicy(policy: String) {
        syncManager.setAutoBackupPolicy(policy)
    }

    suspend fun listCloudBackups(): List<DriveFileInfo> {
        return try {
            val signInManagerImpl = signInManager as? com.example.core.drive.GoogleSignInManagerImpl
            val driveService = com.example.core.drive.GoogleDriveServiceImpl(getApplication(), signInManager)
            driveService.listAppDataFiles()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun restoreSpecificBackup(fileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val driveService = com.example.core.drive.GoogleDriveServiceImpl(getApplication(), signInManager)
                val cacheDir = File(getApplication<Application>().cacheDir, "staging")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val tempFile = File(cacheDir, "historic_backup_temp.bin")
                val success = driveService.downloadFromAppData(fileId, tempFile)
                if (success) {
                    val db = AppDatabase.getDatabase(getApplication())
                    val backupText = tempFile.readBytes()
                    // Restore can use workspaceManager or standard restoreManager. Since they are raw backups, restoreManager is ideal
                    val restoreResult = workspaceManager.restoreWorkspace(tempFile)
                    tempFile.delete()
                    onComplete(restoreResult)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun checkForNewerBackup(onResult: (BackupMetadata?) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.checkForNewerBackup()
            onResult(result)
        }
    }

    fun signIn(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = signInManager.signIn(getApplication())
            onResult(success)
        }
    }

    fun signOut(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = signInManager.signOut()
            onResult(success)
        }
    }

    fun exportWorkspaceBundle(destinationFile: File, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = workspaceManager.exportWorkspace(destinationFile)
                onComplete(file)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    fun verifyAndPreviewWorkspaceBundle(file: File, onComplete: (WorkspacePreview) -> Unit) {
        viewModelScope.launch {
            val preview = workspaceManager.verifyAndPreviewWorkspace(file)
            onComplete(preview)
        }
    }

    fun importWorkspaceBundle(rawJsonText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = workspaceManager.importWorkspace(rawJsonText)
            onComplete(success)
        }
    }
}

class SettingsViewModelFactory(
    private val application: Application,
    private val repository: QuotesRepository,
    private val syncManager: SyncManager,
    private val workspaceManager: WorkspaceManager,
    private val deviceManager: DeviceManager,
    private val signInManager: GoogleSignInManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                application,
                repository,
                syncManager,
                workspaceManager,
                deviceManager,
                signInManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
