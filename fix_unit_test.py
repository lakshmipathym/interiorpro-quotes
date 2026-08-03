import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

# Make sure we're getting the compilation right
lines = content.split('\n')
for i, line in enumerate(lines):
    if "balance =" in line:
        print(f"{i}: {line}")
