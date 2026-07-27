import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Replace any occurrence of canonical company name logic
content = content.replace("val canonicalCompanyName = company.companyName.trim().ifBlank { \"Company Name\" }", "")
content = content.replace("val companyNameText = company.companyName.trim().ifBlank { \"Company Name\" }", "val canonicalCompanyName = company.companyName.trim().ifBlank { \"Company Name\" }\n            val companyNameText = canonicalCompanyName")
content = content.replace("val coNameVal = company.companyName.trim().ifBlank { \"Company Name\" }", "val coNameVal = canonicalCompanyName")

# Footer
content = content.replace("company.companyName.trim().ifBlank { \"Company Name\" },\n            36f,\n            footY + 12f", "canonicalCompanyName,\n            36f,\n            footY + 12f")

# Terms and conditions
# We need to find the terms & conditions drawing logic and the signature block.
with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
