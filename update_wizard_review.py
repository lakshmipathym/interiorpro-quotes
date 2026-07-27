import re

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

# 1. Update the call to WizardStepReview
old_call = """                    else -> WizardStepReview(
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        customerAddress = currentCustomer?.address ?: "",
                        siteLocation = currentCustomer?.siteLocation ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstRate = gstRate,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        terms = termsAndConditions,
                        warranty = warranty,
                        onDiscountChange = { quotationViewModel.setDiscount(it) },
                        onGstRateChange = { quotationViewModel.setGstRate(it) },
                        onTermsChange = { quotationViewModel.setTerms(it) },
                        onWarrantyChange = { quotationViewModel.setWarranty(it) },
                        onSave = {"""

new_call = """                    else -> WizardStepReview(
                        quotationViewModel = quotationViewModel,
                        quoteNumber = quoteNumber,
                        customerName = currentCustomer?.customerName ?: "Unknown",
                        customerPhone = currentCustomer?.mobileNumber ?: "",
                        customerAddress = currentCustomer?.address ?: "",
                        siteLocation = currentCustomer?.siteLocation ?: "",
                        itemsCount = quoteItems.size,
                        subtotal = subtotal,
                        discount = discount,
                        gstRate = gstRate,
                        gstAmount = gstAmount,
                        grandTotal = grandTotal,
                        terms = termsAndConditions,
                        warranty = warranty,
                        onSave = {"""

content = content.replace(old_call, new_call)

# 2. Find WizardStepReview definition and replace it completely
start_marker = "fun WizardStepReview("
# We want to replace it until the end of the file or next major section.
# Let's find "fun WizardStepReview(" and replace it until the end of the file.
# No, let's just find the end of its block. It's the last function in NewQuotationScreen.kt, but we can't be sure.
# I'll just write a regex to replace it.

end_marker = "fun ItemConfigDialog"

