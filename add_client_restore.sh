sed -i '/\/\/ 3\. Restore Master Data/i\
                \/\/ 2.1 Restore Clients (Legacy) into Customer\
                if (root.has("clients")) {\
                    val clientArr = root.getJSONArray("clients")\
                    for (i in 0 until clientArr.length()) {\
                        val clObj = clientArr.getJSONObject(i)\
                        \
                        \/\/ Map to CustomerEntity\
                        val c = CustomerEntity(\
                            customerId = 0, \/\/ Let Room auto-generate\
                            customerName = clObj.optString("clientName", ""), \
                            companyName = clObj.optString("companyName", ""),\
                            contactPerson = clObj.optString("contactPerson", ""),\
                            mobileNumber = clObj.optString("mobileNumber", ""),\
                            whatsappNumber = clObj.optString("whatsappNumber", ""),\
                            email = clObj.optString("email", ""),\
                            address = clObj.optString("address", ""),\
                            siteLocation = clObj.optString("siteLocation", ""),\
                            city = clObj.optString("city", ""),\
                            district = clObj.optString("district", ""),\
                            state = clObj.optString("state", ""),\
                            pincode = clObj.optString("pincode", ""),\
                            country = clObj.optString("country", "India"),\
                            gstin = clObj.optString("gstin", ""),\
                            notes = clObj.optString("notes", ""),\
                            isActive = clObj.optBoolean("isActive", true),\
                            createdDate = clObj.optLong("createdDate", System.currentTimeMillis()),\
                            modifiedDate = clObj.optLong("modifiedDate", System.currentTimeMillis())\
                        )\
                        db.customerDao().insertCustomer(c)\
                    }\
                }\
' app/src/main/java/com/example/backup/BackupManager.kt
