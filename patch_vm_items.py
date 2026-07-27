with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("_quoteItems.value.isEmpty()", "_newQuoteItems.value.isEmpty()")

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "w") as f:
    f.write(content)
