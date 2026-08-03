import re

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    content = f.read()

# Restore clean HistoryViewModel
content = content.replace("println(\"Update Status called: $id $status\"); val current = repository.getQuotationByIdDirect(id) ?: return@launch; println(\"Current status: ${current.status}\")", "val current = repository.getQuotationByIdDirect(id) ?: return@launch")
content = content.replace("println(\"Got items\"); val items = repository.getQuotationItemsDirect(id)", "val items = repository.getQuotationItemsDirect(id)")
content = content.replace("println(\"Got customer entity\"); val customerEntity = repository.getCustomerById(current.customerId) ?: run {\\nprintln(\"Customer Not Found! ID: ${current.customerId}\")\\nreturn@launch\\n}", "val customerEntity = repository.getCustomerById(current.customerId) ?: return@launch")
content = content.replace("println(\"Got company profile\"); val companyProfile = repository.getCompanyProfileDirect()", "val companyProfile = repository.getCompanyProfileDirect()")
content = content.replace("println(\"Calculated quotation\"); val calculatedQuotation = calculateQuotationUseCase.execute(rawInput, rawItems)", "val calculatedQuotation = calculateQuotationUseCase.execute(rawInput, rawItems)")
content = content.replace("println(\"Calling finalize\"); finalizeQuotationUseCase.execute(", "finalizeQuotationUseCase.execute(")
content = content.replace("println(\"Saving quotation\"); repository.saveQuotationWithItems(", "repository.saveQuotationWithItems(")

content = content.replace("e.printStackTrace(); println(\"MY_ERROR: ${e.message}\"); println(e)", "// Do not update status on failure")

# Make it use Dispatchers.IO
content = content.replace("fun updateQuotationStatus(id: Int, status: String) {\\n        viewModelScope.launch {", "fun updateQuotationStatus(id: Int, status: String) {\\n        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {")

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(content)
