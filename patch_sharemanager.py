import re

with open('app/src/main/java/com/example/utils/ShareManager.kt', 'r') as f:
    content = f.read()

old_code = """        // Generate the PDF
        if (snapshot != null) {
            val company = repository.getCompanyProfileDirect() ?: com.example.data.CompanyProfile()
            com.example.pdf.PdfGenerator.generateQuotationPdf(context, company, snapshot, file)
        } else {"""

new_code = """        // Generate the PDF
        if (snapshot != null) {
            val company = com.example.data.CompanyProfile(
                companyName = snapshot.company.companyName,
                ownerName = snapshot.company.ownerName,
                contactPerson = "",
                phone = snapshot.company.phone,
                whatsappNumber = snapshot.company.whatsappNumber,
                email = snapshot.company.email,
                website = snapshot.company.website,
                address = snapshot.company.address,
                city = "",
                district = "",
                state = "",
                pincode = "",
                bankName = snapshot.company.bankName,
                accountHolderName = snapshot.company.accountHolderName,
                accountNumber = snapshot.company.accountNumber,
                ifsc = snapshot.company.ifsc,
                branch = snapshot.company.branch,
                upiId = snapshot.company.upiId,
                logoPath = snapshot.company.logoPath,
                gstin = snapshot.company.gstin,
                signatureText = "",
                signaturePath = snapshot.company.signaturePath,
                tagline = "",
                companySealPath = snapshot.company.companySealPath,
                brandColor = "",
                defaultGstRate = 0.0,
                defaultDiscount = 0.0,
                defaultValidityDays = 0,
                defaultDeliveryDays = 0,
                termsAndConditions = "",
                defaultWarranty = "",
                defaultDeliveryTime = "",
                defaultInstallationTime = "",
                defaultPaymentTerms = "",
                defaultQuoteValidity = "",
                additionalConditions = ""
            )
            com.example.pdf.PdfGenerator.generateQuotationPdf(context, company, snapshot, file)
        } else {"""

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('app/src/main/java/com/example/utils/ShareManager.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Failed to find code block in ShareManager")
