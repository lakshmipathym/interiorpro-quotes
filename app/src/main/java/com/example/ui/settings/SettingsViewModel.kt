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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsViewModel(
    application: Application,
    private val repository: QuotesRepository,
    val syncManager: SyncManager,
    val workspaceManager: WorkspaceManager,
    val deviceManager: DeviceManager,
    val signInManager: GoogleSignInManager
) : AndroidViewModel(application) {

    private val backupPrefs = application.getSharedPreferences("backup_prefs", android.content.Context.MODE_PRIVATE)

    private val _lastBackupDate = MutableStateFlow(backupPrefs.getString("last_cloud_backup_date", "Never") ?: "Never")
    val lastBackupDate: StateFlow<String> = _lastBackupDate.asStateFlow()

    private val _lastBackupFileName = MutableStateFlow(backupPrefs.getString("last_cloud_backup_filename", "None") ?: "None")
    val lastBackupFileName: StateFlow<String> = _lastBackupFileName.asStateFlow()

    private val _lastBackupStatus = MutableStateFlow(backupPrefs.getString("last_cloud_backup_status", "Idle") ?: "Idle")
    val lastBackupStatus: StateFlow<String> = _lastBackupStatus.asStateFlow()

    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    private val _cloudBackupsList = MutableStateFlow<List<DriveFileInfo>>(emptyList())
    val cloudBackupsList: StateFlow<List<DriveFileInfo>> = _cloudBackupsList.asStateFlow()

    private val _isLoadingCloudBackups = MutableStateFlow(false)
    val isLoadingCloudBackups: StateFlow<Boolean> = _isLoadingCloudBackups.asStateFlow()

    val allTemplates: StateFlow<List<QuotationTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncState: StateFlow<SyncState> = syncManager.syncState
    val googleIdentityManager = com.example.core.identity.GoogleIdentityManager(application, signInManager)
    val googleIdentityState = googleIdentityManager.identityState
    val deviceBindingManager = com.example.core.device.DeviceBindingManager(application)
    val deviceBindingInfo = deviceBindingManager.bindingInfo
    val cloudLicenseValidator = com.example.core.license.CloudLicenseValidator(application)
    val cloudLicenseState = cloudLicenseValidator.cloudState

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    fun triggerCloudLicenseSync(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val state = cloudLicenseValidator.verifyOrRegisterCloudLicense()
                _isCloudSyncing.value = false
                onComplete(state.isVerifiedOnline, state.syncStatusMessage)
            } catch (e: Exception) {
                _isCloudSyncing.value = false
                onComplete(false, e.message ?: "Cloud verification failed")
            }
        }
    }

    fun activateSubscriptionPlan(
        plan: com.example.core.license.SubscriptionPlan,
        licenseKey: String? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val state = cloudLicenseValidator.activateSubscriptionPlan(plan, licenseKey)
                _isCloudSyncing.value = false
                onComplete(state.isVerifiedOnline, state.syncStatusMessage)
            } catch (e: Exception) {
                _isCloudSyncing.value = false
                onComplete(false, e.message ?: "Plan activation failed")
            }
        }
    }

    val isUserSignedIn: StateFlow<Boolean> = signInManager.isUserSignedIn
    val currentUserEmail: StateFlow<String?> = signInManager.currentUserEmail
    val currentUserDisplayName: StateFlow<String?> = signInManager.currentUserDisplayName

    private val _isSignInLoading = MutableStateFlow(false)
    val isSignInLoading: StateFlow<Boolean> = _isSignInLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

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
            emptyList()
        }
    }

    fun fetchCloudBackupsList() {
        viewModelScope.launch {
            _isLoadingCloudBackups.value = true
            val list = listCloudBackups()
            _cloudBackupsList.value = list
            _isLoadingCloudBackups.value = false
        }
    }

    fun performBackupToGoogleDrive(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isBackupInProgress.value = true
            try {
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val timestampStr = sdf.format(Date())
                val fileName = "backup_$timestampStr.ipro"
                val cacheDir = File(getApplication<Application>().cacheDir, "staging")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val stagingFile = File(cacheDir, fileName)

                val exportedFile = workspaceManager.exportWorkspace(stagingFile)
                val driveService = com.example.core.drive.GoogleDriveServiceImpl(getApplication(), signInManager)
                val displaySdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = displaySdf.format(Date())

                val metadataMap = mapOf(
                    "timestamp" to System.currentTimeMillis().toString(),
                    "appVersion" to deviceManager.getAppVersion(),
                    "databaseVersion" to deviceManager.getDatabaseVersion().toString(),
                    "deviceId" to deviceManager.getDeviceId(),
                    "deviceName" to deviceManager.getDeviceName()
                )

                val fileId = driveService.uploadToAppData(exportedFile, "application/octet-stream", metadataMap)

                if (fileId.isNotEmpty()) {
                    backupPrefs.edit()
                        .putString("last_cloud_backup_date", formattedDate)
                        .putString("last_cloud_backup_filename", fileName)
                        .putString("last_cloud_backup_status", "Success")
                        .apply()

                    _lastBackupDate.value = formattedDate
                    _lastBackupFileName.value = fileName
                    _lastBackupStatus.value = "Success"

                    if (stagingFile.exists()) stagingFile.delete()
                    _isBackupInProgress.value = false
                    onComplete(true, "Backup uploaded successfully to Google Drive ($fileName)")
                } else {
                    throw Exception("Upload returned empty file ID")
                }
            } catch (e: Exception) {
                val displaySdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDate = displaySdf.format(Date())
                backupPrefs.edit()
                    .putString("last_cloud_backup_status", "Failed")
                    .apply()
                _lastBackupStatus.value = "Failed"
                _isBackupInProgress.value = false
                onComplete(false, e.message ?: "Backup to Google Drive failed")
            }
        }
    }

    fun restoreSpecificBackup(fileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cacheDir = File(getApplication<Application>().cacheDir, "staging")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val tempFile = File(cacheDir, "historic_backup_temp.bin")
            try {
                val driveService = com.example.core.drive.GoogleDriveServiceImpl(getApplication(), signInManager)
                val success = driveService.downloadFromAppData(fileId, tempFile)
                if (success) {
                    val restoreResult = workspaceManager.restoreWorkspace(tempFile)
                    onComplete(restoreResult)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                onComplete(false)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    fun checkForNewerBackup(onResult: (BackupMetadata?) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.checkForNewerBackup()
            onResult(result)
        }
    }

    fun signIn(activityContext: android.content.Context, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSignInLoading.value = true
            _authErrorMessage.value = null
            try {
                val success = googleIdentityManager.connectAccount(activityContext)
                if (!success) {
                    _authErrorMessage.value = "Google Account connection failed. Please try again."
                }
                onResult(success)
            } catch (e: Exception) {
                _authErrorMessage.value = e.message ?: "Google Account connection error occurred."
                onResult(false)
            } finally {
                _isSignInLoading.value = false
            }
        }
    }

    fun signOut(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSignInLoading.value = true
            _authErrorMessage.value = null
            try {
                val success = googleIdentityManager.disconnectAccount()
                if (!success) {
                    _authErrorMessage.value = "Disconnecting Google Account failed. Please try again."
                }
                onResult(success)
            } catch (e: Exception) {
                _authErrorMessage.value = e.message ?: "Disconnect error occurred."
                onResult(false)
            } finally {
                _isSignInLoading.value = false
            }
        }
    }

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    fun exportWorkspaceBundle(destinationFile: File, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = workspaceManager.exportWorkspace(destinationFile)
                onComplete(file)
            } catch (e: Exception) {
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
