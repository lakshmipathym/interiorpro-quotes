import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

start_idx = content.find("fun saveQuotation(onComplete: (Int) -> Unit) {")
end_idx = content.find("fun loadQuotationToEdit", start_idx)

new_func = """fun saveQuotation(onComplete: (Int) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val customerEntity = _newQuoteCustomer.value ?: return@launch
            val quoteNumber = _newQuoteNumber.value.ifEmpty { repository.generateNextQuotationNumber() }
            val qIdStr = _editingQuotationId.value?.toString() ?: "0"

            val companyProfile = masterRepository.getCompanyProfileDirect() ?: com.example.data.CompanyProfile()
            
            val customerSnapshot = com.example.domain.models.CustomerSnapshot(
                customerId = customerEntity.customerId.toString(),
                customerName = customerEntity.customerName,
                customerPhone = customerEntity.mobileNumber,
                customerAddress = customerEntity.address,
                siteName = _newQuoteSiteName.value,
                siteAddress = _newQuoteSiteAddress.value
            )
            
            val companySnapshot = com.example.domain.models.CompanySnapshot(
                companyName = companyProfile.companyName,
                ownerName = companyProfile.ownerName,
                phone = companyProfile.phoneNumber,
                email = companyProfile.email,
                address = companyProfile.address,
                gstin = companyProfile.gstin,
                bankName = companyProfile.bankName,
                accountHolderName = companyProfile.accountName,
                accountNumber = companyProfile.accountNumber,
                ifsc = companyProfile.ifscCode,
                branch = companyProfile.branch,
                upiId = companyProfile.upiId,
                website = companyProfile.website,
                whatsapp = companyProfile.whatsappNumber
            )

            val rawInput = com.example.domain.models.RawQuotationInput(
                discount = _newQuoteDiscount.value,
                gstRate = _newQuoteGstRate.value,
                transport = _newQuoteTransport.value,
                installation = _newQuoteInstallation.value,
                extraCharges = _newQuoteExtraCharges.value,
                roundOff = _newQuoteRoundOff.value,
                advance = _newQuoteAdvance.value
            )
            
            val calcQuote = calculatedQuotation.value

            finalizeQuotationUseCase.execute(
                id = qIdStr,
                quotationNumber = quoteNumber,
                date = _newQuoteDate.value,
                customer = customerSnapshot,
                company = companySnapshot,
                termsAndConditions = _newQuoteTerms.value,
                warranty = _newQuoteWarranty.value,
                validityDays = _newQuoteValidityDays.value,
                notes = _newQuoteCustomerNotes.value,
                rawInput = rawInput,
                calculatedQuotation = calcQuote
            )
            
            val savedSnapshot = snapshotRepository.getSnapshotByNumber(quoteNumber)
            val newId = savedSnapshot?.id?.toIntOrNull() ?: _editingQuotationId.value ?: 0
            
            syncManager.onQuotationSaved()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(newId)
            }
        }
    }

    """

content = content[:start_idx] + new_func + content[end_idx:]

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'w') as f:
    f.write(content)
