import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

old_code = """        val customer = if (isSnapshotMode || quotation.status == "FINALIZED") {
            com.example.data.CustomerEntity(
                customerId = quotation.customerId.toLong(),
                customerName = quotation.customerName,
                mobileNumber = quotation.customerPhone,
                address = quotation.customerAddress,
                siteLocation = quotation.siteName,
                siteAddress = quotation.siteAddress,
                customerEmail = quotation.customerEmail,
                customerWhatsapp = quotation.customerWhatsapp,
                customerContactPerson = quotation.customerContactPerson,
                customerCompanyName = quotation.customerCompanyName,
                customerGstin = quotation.customerGstin
            )"""

new_code = """        val customer = if (isSnapshotMode || quotation.status == "FINALIZED") {
            com.example.data.CustomerEntity(
                customerId = quotation.customerId.toLong(),
                customerName = quotation.customerName,
                mobileNumber = quotation.customerPhone,
                address = quotation.customerAddress,
                siteLocation = quotation.siteName,
                siteAddress = quotation.siteAddress,
                email = quotation.customerEmail,
                whatsappNumber = quotation.customerWhatsapp,
                contactPerson = quotation.customerContactPerson,
                companyName = quotation.customerCompanyName,
                gstin = quotation.customerGstin
            )"""

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Could not find code")

