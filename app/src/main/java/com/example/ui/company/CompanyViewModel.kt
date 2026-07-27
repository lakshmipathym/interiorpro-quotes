package com.example.ui.company

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompanyViewModel(application: Application, private val repository: QuotesRepository, private val masterRepository: com.example.data.MasterRepository) : AndroidViewModel(application) {
    val companyProfile: StateFlow<CompanyProfile?> = repository.companyProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val allMasterData: StateFlow<List<com.example.data.MasterEntity>> = masterRepository.getAllMasters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCompanyProfile(profile: CompanyProfile) {
        viewModelScope.launch {
            repository.saveCompanyProfile(profile)
        }
    }

    fun getMasterDataByType(type: String): Flow<List<com.example.data.MasterEntity>> = masterRepository.getMastersByType(type)

    fun saveMasterData(type: String, value: String, extra: String = "") {
        viewModelScope.launch {
            masterRepository.saveMaster(com.example.data.MasterEntity(masterType = type, name = value, description = extra))
        }
    }

    fun deleteMasterData(master: com.example.data.MasterEntity) {
        viewModelScope.launch {
            masterRepository.deleteMasterPermanently(master)
        }
    }

    fun deleteMasterDataById(id: Long) {
        viewModelScope.launch {
            masterRepository.softDeleteMaster(id)
        }
    }
}

class CompanyViewModelFactory(private val application: Application, private val repository: QuotesRepository, private val masterRepository: com.example.data.MasterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompanyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CompanyViewModel(application, repository, masterRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
