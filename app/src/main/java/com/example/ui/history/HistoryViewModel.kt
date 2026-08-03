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

class HistoryViewModel(
    application: Application,
    val repository: QuotesRepository,
    private val calculateQuotationUseCase: com.example.domain.usecases.CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: com.example.domain.usecases.FinalizeQuotationUseCase
) : AndroidViewModel(application) {
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val current = repository.getQuotationByIdDirect(id) ?: return@launch
            if (status.equals("Final", ignoreCase = true) || status.equals("FINALIZED", ignoreCase = true)) {
                if (current.status.equals("Final", ignoreCase = true) || current.status.equals("FINALIZED", ignoreCase = true)) return@launch
                try {
                    val items = repository.getQuotationItemsDirect(id)
                    val customerEntity = repository.getCustomerById(current.customerId) ?: return@launch
                    val companyProfile = repository.getCompanyProfileDirect() ?: com.example.data.CompanyProfile()
                    
                    val customerSnapshot = com.example.domain.models.CustomerSnapshot(
                        customerId = customerEntity.customerId.toString(),
                        customerName = customerEntity.customerName,
                        customerPhone = customerEntity.mobileNumber,
                        customerAddress = customerEntity.address,
                        siteName = current.siteName,
                        siteAddress = current.siteAddress,
                        customerEmail = customerEntity.email,
                        customerWhatsapp = customerEntity.whatsappNumber,
                        contactPerson = customerEntity.contactPerson,
                        companyName = customerEntity.companyName,
                        gstin = customerEntity.gstin,
                        projectName = current.projectName
                    )
                    
                    val companySnapshot = com.example.domain.models.CompanySnapshot(
                        companyName = companyProfile.companyName,
                        ownerName = companyProfile.ownerName,
                        phone = companyProfile.phone,
                        email = companyProfile.email,
                        address = companyProfile.address,
                        gstin = companyProfile.gstin,
                        bankName = companyProfile.bankName,
                        accountHolderName = companyProfile.accountHolderName,
                        accountNumber = companyProfile.accountNumber,
                        ifsc = companyProfile.ifsc,
                        branch = companyProfile.branch,
                        upiId = companyProfile.upiId,
                        website = companyProfile.website,
                        whatsappNumber = companyProfile.whatsappNumber,
                        logoPath = companyProfile.logoPath,
                        signaturePath = companyProfile.signaturePath,
                        companySealPath = companyProfile.companySealPath
                    )
                    
                    val rawInput = com.example.domain.models.RawQuotationInput(
                        discount = current.discount,
                        gstRate = current.gstRate,
                        transport = current.transport,
                        installation = current.installation,
                        extraCharges = current.extraCharges,
                        roundOff = current.roundOff,
                        advance = current.advance
                    )
                    
                    val rawItems = items.map {
                        val parts = it.description.split("|||")
                        val specsJson = if (parts.size > 1) parts[1].trim() else "{}"
                        var w = "0"; var h = "0"; var d = "0"
                        try {
                            if (specsJson.startsWith("{") && specsJson.endsWith("}")) {
                                val json = org.json.JSONObject(specsJson)
                                w = json.optString("width", "0")
                                h = json.optString("height", "0")
                                d = json.optString("depth", "0")
                            }
                        } catch (e: Exception) {}
                        
                        com.example.domain.models.RawItemInput(
                            itemName = it.itemName,
                            description = it.description,
                            material = it.material,
                            finish = it.finish,
                            width = w,
                            height = h,
                            depth = d,
                            unit = it.unit,
                            quantity = it.quantity,
                            rate = it.rate
                        )
                    }
                    
                    val calculatedQuotation = calculateQuotationUseCase.execute(rawInput, rawItems)
                    
                    finalizeQuotationUseCase.execute(
                        id = current.id.toString(),
                        quotationNumber = current.quotationNumber,
                        date = current.date,
                        customer = customerSnapshot,
                        company = companySnapshot,
                        termsAndConditions = current.termsAndConditions,
                        warranty = current.warranty,
                        deliveryTime = current.deliveryTime,
                        installationTime = current.installationTime,
                        paymentTerms = current.paymentTerms,
                        additionalConditions = current.additionalConditions,
                        validityDays = current.validityDays,
                        notes = current.customerNotes,
                        rawInput = rawInput,
                        calculatedQuotation = calculatedQuotation
                    )
                    
                    repository.saveQuotationWithItems(
                        current.copy(status = "Final"),
                        items
                    )
                } catch (e: Exception) {
                    // Do not update status on failure
                }
            } else {
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

class HistoryViewModelFactory(
    private val application: Application,
    private val repository: QuotesRepository,
    private val calculateQuotationUseCase: com.example.domain.usecases.CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: com.example.domain.usecases.FinalizeQuotationUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(application, repository, calculateQuotationUseCase, finalizeQuotationUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
