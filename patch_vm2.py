import re

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("masterRepository.getCompanyProfileDirect()", "repository.getCompanyProfileDirect()")
content = content.replace("phone = companyProfile.phoneNumber,", "phone = companyProfile.phone,")
content = content.replace("accountHolderName = companyProfile.accountName,", "accountHolderName = companyProfile.accountHolderName,")
content = content.replace("ifsc = companyProfile.ifscCode,", "ifsc = companyProfile.ifsc,")
content = content.replace("whatsapp = companyProfile.whatsappNumber", "whatsappNumber = companyProfile.whatsappNumber")

with open('app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt', 'w') as f:
    f.write(content)
