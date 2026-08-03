import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

old_code = """        // Query database for complete client details
        val db = AppDatabase.getDatabase(context)
        val customer = kotlinx.coroutines.runBlocking {
            try {
                db.customerDao().getCustomerById(quotation.customerId.toLong())
            } catch (e: Exception) {
                null
            }
        }"""

new_code = """        val customer = if (isSnapshotMode || quotation.status == "FINALIZED") {
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
            )
        } else {
            // Query database for complete client details
            val db = com.example.data.AppDatabase.getDatabase(context)
            kotlinx.coroutines.runBlocking {
                try {
                    db.customerDao().getCustomerById(quotation.customerId.toLong())
                } catch (e: Exception) {
                    null
                }
            }
        }"""

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Could not find code")

