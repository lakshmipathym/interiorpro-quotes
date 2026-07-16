package com.example.ui.history

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

class HistoryViewModel(application: Application, val repository: QuotesRepository) : AndroidViewModel(application) {
    val allQuotations: StateFlow<List<Quotation>> = repository.allQuotations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getQuotationItems(quotationId: Int): Flow<List<QuotationItem>> = repository.getQuotationItems(quotationId)

    fun deleteQuotation(id: Int) {
        viewModelScope.launch {
            repository.deleteQuotation(id)
        }
    }

    fun updateQuotationStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.getQuotationByIdDirect(id)?.let { current ->
                repository.saveQuotationWithItems(
                    current.copy(status = status),
                    repository.getQuotationItemsDirect(id)
                )
            }
        }
    }

    fun duplicateQuotation(id: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val original = repository.getQuotationByIdDirect(id) ?: return@launch
            val items = repository.getQuotationItemsDirect(id)
            val newNumber = repository.generateNextQuotationNumber()
            
            val duplicated = original.copy(
                id = 0,
                quotationNumber = newNumber,
                date = System.currentTimeMillis(),
                status = "Draft"
            )
            
            val duplicatedItems = items.map {
                it.copy(
                    id = 0,
                    quotationId = 0
                )
            }
            
            repository.saveQuotationWithItems(duplicated, duplicatedItems)
            onComplete(newNumber)
        }
    }
}

class HistoryViewModelFactory(private val application: Application, private val repository: QuotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
