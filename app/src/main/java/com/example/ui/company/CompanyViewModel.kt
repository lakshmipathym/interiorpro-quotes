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

class CompanyViewModel(application: Application, private val repository: QuotesRepository) : AndroidViewModel(application) {
    val companyProfile: StateFlow<CompanyProfile?> = repository.companyProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val allMasterData: StateFlow<List<MasterData>> = repository.allMasterData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCompanyProfile(profile: CompanyProfile) {
        viewModelScope.launch {
            repository.saveCompanyProfile(profile)
        }
    }

    fun getMasterDataByType(type: String): Flow<List<MasterData>> = repository.getMasterDataByType(type)

    fun saveMasterData(type: String, value: String, extra: String = "") {
        viewModelScope.launch {
            repository.saveMasterData(MasterData(type = type, value = value, extra = extra))
        }
    }

    fun deleteMasterData(master: MasterData) {
        viewModelScope.launch {
            repository.deleteMasterData(master)
        }
    }

    fun deleteMasterDataById(id: Int) {
        viewModelScope.launch {
            repository.deleteMasterDataById(id)
        }
    }
}

class CompanyViewModelFactory(private val application: Application, private val repository: QuotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompanyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CompanyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
