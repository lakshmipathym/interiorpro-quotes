import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("balance = calcQuote.balance,", "balance = calcQuote.balanceDue,")
content = content.replace("unit = item.unit.name,", "unit = item.unit,")

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'w') as f:
    f.write(content)
