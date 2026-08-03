import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "fun saveQuotation(" in line:
        start = i
        break

for i in range(start, start + 70):
    print(lines[i])
