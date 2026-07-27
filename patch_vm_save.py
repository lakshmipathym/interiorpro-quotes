import sys
import re

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "r") as f:
    content = f.read()

old_save_block = """    fun saveQuotation(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val customer = _newQuoteCustomer.value ?: return@launch
            val quote = Quotation(
                id = _editingQuotationId.value ?: 0,
                quotationNumber = _newQuoteNumber.value.ifEmpty { repository.generateNextQuotationNumber() },
                date = _newQuoteDate.value,
                customerId = customer.customerId,
                customerName = customer.customerName,
                customerPhone = customer.mobileNumber,
                customerAddress = customer.address,
                siteName = _newQuoteSiteName.value,
                siteAddress = _newQuoteSiteAddress.value,
                projectName = _newQuoteProjectName.value,
                projectType = _newQuoteProjectType.value,
                category = _newQuoteCategory.value,
                material = _newQuoteMaterial.value,
                finish = _newQuoteFinish.value,
                subtotal = newQuoteSubtotal.value,
                discount = _newQuoteDiscount.value,
                gstRate = _newQuoteGstRate.value,
                gstAmount = newQuoteGstAmount.value,
                transport = _newQuoteTransport.value,
                installation = _newQuoteInstallation.value,
                extraCharges = _newQuoteExtraCharges.value,
                roundOff = _newQuoteRoundOff.value,
                grandTotal = newQuoteGrandTotal.value,
                advance = _newQuoteAdvance.value,
                balance = newQuoteBalance.value,
                termsAndConditions = _newQuoteTerms.value,
                warranty = _newQuoteWarranty.value,
                customerNotes = _newQuoteCustomerNotes.value,
                internalNotes = _newQuoteInternalNotes.value,
                validityDays = _newQuoteValidityDays.value,
                status = _editingQuotationStatus.value
            )
            val qId = repository.saveQuotationWithItems(quote, _newQuoteItems.value)
            syncManager.onQuotationSaved()
            onComplete(qId)
        }
    }"""

new_save_block = """    fun saveQuotation(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val customerEntity = _newQuoteCustomer.value ?: return@launch
            val companyProfile = repository.getCompanyProfileDirect() ?: return@launch
            
            val quoteNumber = _newQuoteNumber.value.ifEmpty { repository.generateNextQuotationNumber() }
            val qIdStr = (_editingQuotationId.value ?: 0).toString()

            val customerSnapshot = CustomerSnapshot(
                customerId = customerEntity.customerId,
                customerName = customerEntity.customerName,
                customerPhone = customerEntity.mobileNumber,
                customerAddress = customerEntity.address,
                siteName = _newQuoteSiteName.value,
                siteAddress = _newQuoteSiteAddress.value
            )

            val companySnapshot = CompanySnapshot(
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
                upiId = companyProfile.upiId
            )

            val rawInput = RawQuotationInput(
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
            
            // To maintain compatibility with legacy onComplete(Int), fetch the saved snapshot
            val savedSnapshot = snapshotRepository.getSnapshotByNumber(quoteNumber)
            val newId = savedSnapshot?.id?.toIntOrNull() ?: _editingQuotationId.value ?: 0
            
            syncManager.onQuotationSaved()
            onComplete(newId)
        }
    }"""

if old_save_block in content:
    content = content.replace(old_save_block, new_save_block)
else:
    print("Could not find old_save_block")
    sys.exit(1)

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "w") as f:
    f.write(content)

