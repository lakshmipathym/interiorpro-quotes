with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "r") as f:
    content = f.read()

old_validation = """                val step1Error = when {
                    currentCustomer == null -> "Select a customer"
                    siteName.isBlank() -> "Enter Site Name"
                    siteAddress.isBlank() -> "Enter Site Address"
                    else -> null
                }
                
                val step2Error = when {
                    quoteItems.isEmpty() -> "Add at least one item"
                    else -> null
                }
                
                val validationError = when (activeStep) {
                    1 -> step1Error
                    2 -> step2Error
                    else -> null
                }"""

new_validation = """                val validationError = quotationViewModel.validateStep(activeStep)"""

if old_validation in content:
    content = content.replace(old_validation, new_validation)
    with open("app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt", "w") as f:
        f.write(content)
    print("Patched UI successfully")
else:
    print("UI target not found")
