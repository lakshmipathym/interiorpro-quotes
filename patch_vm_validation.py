with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "r") as f:
    content = f.read()

validation_code = """
    fun validateStep(step: Int): String? {
        return when (step) {
            1 -> {
                when {
                    _newQuoteCustomer.value == null -> "Select a customer to continue"
                    _newQuoteSiteName.value.isBlank() -> "Enter Site Name to continue"
                    _newQuoteSiteAddress.value.isBlank() -> "Enter Site Address to continue"
                    else -> null
                }
            }
            2 -> {
                when {
                    _quoteItems.value.isEmpty() -> "Add at least one item to continue"
                    else -> null
                }
            }
            else -> null
        }
    }
"""

if "fun saveQuotation(" in content:
    content = content.replace("fun saveQuotation(", validation_code + "\n    fun saveQuotation(")
    with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "w") as f:
        f.write(content)
    print("Patched VM successfully")
