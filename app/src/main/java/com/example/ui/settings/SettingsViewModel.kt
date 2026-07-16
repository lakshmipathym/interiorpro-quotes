package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application, private val repository: QuotesRepository) : AndroidViewModel(application) {
    val allTemplates: StateFlow<List<QuotationTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}

class SettingsViewModelFactory(private val application: Application, private val repository: QuotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
