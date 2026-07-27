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
            val quotation = repository.getQuotationByIdDirect(id)
            if (quotation != null) {
                val safeQuoteNum = quotation.quotationNumber.replace("/", "_")
                val filesDir = getApplication<Application>().filesDir
                val files = filesDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.name.startsWith("design_${safeQuoteNum}_") || file.name.startsWith("laminate_${safeQuoteNum}_")) {
                            file.delete()
                        }
                    }
                }
            }
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
            
            val safeNewQuoteNum = newNumber.replace("/", "_")
            val duplicatedItems = items.mapIndexed { index, item ->
                var duplicatedDesc = item.description
                try {
                    if (duplicatedDesc.contains("|||")) {
                        val parts = duplicatedDesc.split("|||")
                        val userDesc = parts[0].trim()
                        val specsJson = parts[1].trim()
                        if (specsJson.startsWith("{") && specsJson.endsWith("}")) {
                            val json = org.json.JSONObject(specsJson)
                            
                            val laminateImageUri = json.optString("laminateImageUri", "")
                            if (laminateImageUri.isNotEmpty()) {
                                val filesDir = getApplication<Application>().filesDir
                                val oldFile = java.io.File(filesDir, java.io.File(laminateImageUri).name)
                                if (oldFile.exists()) {
                                    val newFile = java.io.File(oldFile.parent, "laminate_${safeNewQuoteNum}_${index}.jpg")
                                    oldFile.copyTo(newFile, overwrite = true)
                                    json.put("laminateImageUri", newFile.absolutePath)
                                }
                            }
                            
                            val designImageUri = json.optString("designImageUri", "")
                            if (designImageUri.isNotEmpty()) {
                                val filesDir = getApplication<Application>().filesDir
                                val oldFile = java.io.File(filesDir, java.io.File(designImageUri).name)
                                if (oldFile.exists()) {
                                    val newFile = java.io.File(oldFile.parent, "design_${safeNewQuoteNum}_${index}.jpg")
                                    oldFile.copyTo(newFile, overwrite = true)
                                    json.put("designImageUri", newFile.absolutePath)
                                }
                            }
                            
                            duplicatedDesc = "$userDesc ||| $json"
                        }
                    } else if (duplicatedDesc.startsWith("{") && duplicatedDesc.endsWith("}")) {
                        val json = org.json.JSONObject(duplicatedDesc)
                        
                        val laminateImageUri = json.optString("laminateImageUri", "")
                        if (laminateImageUri.isNotEmpty()) {
                            val filesDir = getApplication<Application>().filesDir
                                val oldFile = java.io.File(filesDir, java.io.File(laminateImageUri).name)
                            if (oldFile.exists()) {
                                val newFile = java.io.File(oldFile.parent, "laminate_${safeNewQuoteNum}_${index}.jpg")
                                oldFile.copyTo(newFile, overwrite = true)
                                json.put("laminateImageUri", newFile.absolutePath)
                            }
                        }
                        
                        val designImageUri = json.optString("designImageUri", "")
                        if (designImageUri.isNotEmpty()) {
                            val filesDir = getApplication<Application>().filesDir
                                val oldFile = java.io.File(filesDir, java.io.File(designImageUri).name)
                            if (oldFile.exists()) {
                                val newFile = java.io.File(oldFile.parent, "design_${safeNewQuoteNum}_${index}.jpg")
                                oldFile.copyTo(newFile, overwrite = true)
                                json.put("designImageUri", newFile.absolutePath)
                            }
                        }
                        
                        duplicatedDesc = json.toString()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                
                item.copy(
                    id = 0,
                    quotationId = 0,
                    description = duplicatedDesc
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
