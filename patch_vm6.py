import re

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    content = f.read()

# Extract from start of fun updateQuotationStatus to the end of it
start_idx = content.find("fun updateQuotationStatus")
end_idx = content.find("fun duplicateQuotation")

new_func = """fun updateQuotationStatus(id: Int, status: String) {
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
                        siteAddress = current.siteAddress
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
                        validityDays = current.validityDays,
                        notes = current.customerNotes,
                        rawInput = rawInput,
                        calculatedQuotation = calculatedQuotation
                    )
                    
                    repository.saveQuotationWithItems(
                        current.copy(status = "FINALIZED"),
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

    """

content = content[:start_idx] + new_func + content[end_idx:]

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(content)
