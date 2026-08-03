import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

old_code = "PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)"
new_code = """
            val companyProfile = CompanyProfile(
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
            PdfGenerator.generateQuotationPdf(context, companyProfile, snapshot, outputFile)
"""

content = content.replace(old_code, new_code)

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'w') as f:
    f.write(content)

print("Success")
