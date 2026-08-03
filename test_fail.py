import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("val customerEntity = _newQuoteCustomer.value ?: return@launch", """val customerEntity = _newQuoteCustomer.value
            if (customerEntity == null) {
                println("CUSTOMER ENTITY IS NULL")
                return@launch
            }""")

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'w') as f:
    f.write(content)
