import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

start_idx = content.find("fun saveQuotation(onComplete: (Int) -> Unit) {")
end_idx = content.find("fun loadQuotationToEdit", start_idx)

new_func = """fun saveQuotation(onComplete: (Int) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val customerEntity = _newQuoteCustomer.value ?: return@launch
            val quoteNumber = _newQuoteNumber.value.ifEmpty { repository.generateNextQuotationNumber() }
            val qId = _editingQuotationId.value ?: 0
            val calcQuote = calculatedQuotation.value

            val quotation = com.example.data.Quotation(
                id = qId,
                quotationNumber = quoteNumber,
                date = _newQuoteDate.value,
                status = _editingQuotationStatus.value ?: "DRAFT",
                customerId = customerEntity.customerId,
                customerName = customerEntity.customerName,
                customerPhone = customerEntity.mobileNumber,
                customerAddress = customerEntity.address,
                siteName = _newQuoteSiteName.value,
                siteAddress = _newQuoteSiteAddress.value,
                subtotal = calcQuote.subtotal,
                discount = _newQuoteDiscount.value,
                gstRate = _newQuoteGstRate.value,
                gstAmount = calcQuote.gstAmount,
                transport = _newQuoteTransport.value,
                installation = _newQuoteInstallation.value,
                extraCharges = _newQuoteExtraCharges.value,
                roundOff = _newQuoteRoundOff.value,
                grandTotal = calcQuote.grandTotal,
                advance = _newQuoteAdvance.value,
                balance = calcQuote.balance,
                taxableAmount = calcQuote.taxableAmount,
                amountInWords = calcQuote.amountInWords,
                termsAndConditions = _newQuoteTerms.value,
                warranty = _newQuoteWarranty.value,
                customerNotes = _newQuoteCustomerNotes.value,
                validityDays = _newQuoteValidityDays.value,
                projectName = _newQuoteCategory.value,
                projectType = _newQuoteProjectType.value,
                category = _newQuoteCategory.value,
                material = _newQuoteMaterial.value,
                finish = _newQuoteFinish.value
            )

            val items = _newQuoteItems.value.map { item ->
                com.example.data.QuotationItem(
                    id = if (qId == 0) 0 else item.id, // Only keep ID if we're editing an existing quote
                    quotationId = qId,
                    itemName = item.itemName,
                    description = item.description,
                    material = item.material,
                    finish = item.finish,
                    rawWidth = item.rawWidth,
                    rawHeight = item.rawHeight,
                    rawDepth = item.rawDepth.toString(),
                    parsedWidth = item.parsedWidth,
                    parsedHeight = item.parsedHeight,
                    parsedDepth = item.parsedDepth,
                    rawQuantity = item.rawQuantity,
                    billableQuantity = item.billableQuantity,
                    quantity = item.quantity,
                    unit = item.unit.name,
                    rate = item.rate,
                    amount = item.amount
                )
            }

            val newId = repository.saveQuotationWithItems(quotation, items)
            
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
