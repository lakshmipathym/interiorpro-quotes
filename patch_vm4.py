import re

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val items = repository.getQuotationItemsDirect(id)", "println(\"Got items\"); val items = repository.getQuotationItemsDirect(id)")
content = content.replace("val customerEntity = repository.getCustomerById(current.customerId)", "println(\"Got customer entity\"); val customerEntity = repository.getCustomerById(current.customerId)")
content = content.replace("val companyProfile = repository.getCompanyProfileDirect()", "println(\"Got company profile\"); val companyProfile = repository.getCompanyProfileDirect()")
content = content.replace("val calculatedQuotation = calculateQuotationUseCase.execute(rawInput, rawItems)", "println(\"Calculated quotation\"); val calculatedQuotation = calculateQuotationUseCase.execute(rawInput, rawItems)")
content = content.replace("finalizeQuotationUseCase.execute(", "println(\"Calling finalize\"); finalizeQuotationUseCase.execute(")
content = content.replace("repository.saveQuotationWithItems(", "println(\"Saving quotation\"); repository.saveQuotationWithItems(")

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(content)
