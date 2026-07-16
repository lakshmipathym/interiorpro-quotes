package com.example.backup

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val DEFAULT_AES_KEY = "InteriorProSecureBackupDefault"

    private fun encryptAES(data: String, password: String): String {
        val keyToUse = password.ifEmpty { DEFAULT_AES_KEY }
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(keyToUse.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) // Simple IV for offline encryption
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    private fun decryptAES(encryptedData: String, password: String): String {
        val keyToUse = password.ifEmpty { DEFAULT_AES_KEY }
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(keyToUse.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        val ivSpec = IvParameterSpec(iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }

    suspend fun exportBackup(db: AppDatabase, repository: QuotesRepository, password: String = ""): String {
        val root = JSONObject()
        root.put("backup_version", 1)
        root.put("app_version", "1.0")
        root.put("database_version", 6)
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        root.put("backup_date", sdf.format(Date()))
        root.put("timestamp", System.currentTimeMillis())

        // 1. Company Profile
        val company = repository.getCompanyProfileDirect()
        if (company != null) {
            val coObj = JSONObject()
            coObj.put("companyName", company.companyName)
            coObj.put("contactPerson", company.contactPerson)
            coObj.put("ownerName", company.ownerName)
            coObj.put("phone", company.phone)
            coObj.put("whatsappNumber", company.whatsappNumber)
            coObj.put("email", company.email)
            coObj.put("website", company.website)
            coObj.put("gstin", company.gstin)
            coObj.put("address", company.address)
            coObj.put("city", company.city)
            coObj.put("district", company.district)
            coObj.put("state", company.state)
            coObj.put("pincode", company.pincode)
            coObj.put("bankName", company.bankName)
            coObj.put("accountHolderName", company.accountHolderName)
            coObj.put("accountNumber", company.accountNumber)
            coObj.put("ifsc", company.ifsc)
            coObj.put("branch", company.branch)
            coObj.put("upiId", company.upiId)
            coObj.put("logoPath", company.logoPath)
            coObj.put("signaturePath", company.signaturePath)
            coObj.put("signatureText", company.signatureText)
            coObj.put("tagline", company.tagline)
            coObj.put("companySealPath", company.companySealPath)
            coObj.put("defaultGstRate", company.defaultGstRate)
            coObj.put("defaultDiscount", company.defaultDiscount)
            coObj.put("defaultValidityDays", company.defaultValidityDays)
            coObj.put("defaultDeliveryDays", company.defaultDeliveryDays)
            coObj.put("termsAndConditions", company.termsAndConditions)
            root.put("company_profile", coObj)
        }

        // 2. Customers
        val customers = repository.allCustomers.first()
        val custArr = JSONArray()
        customers.forEach { customer ->
            val cObj = JSONObject()
            cObj.put("customerId", customer.customerId)
            cObj.put("customerName", customer.customerName)
            cObj.put("mobileNumber", customer.mobileNumber)
            cObj.put("whatsappNumber", customer.whatsappNumber)
            cObj.put("email", customer.email)
            cObj.put("address", customer.address)
            cObj.put("siteLocation", customer.siteLocation)
            cObj.put("city", customer.city)
            cObj.put("district", customer.district)
            cObj.put("state", customer.state)
            cObj.put("pincode", customer.pincode)
            cObj.put("notes", customer.notes)
            cObj.put("createdDate", customer.createdDate)
            cObj.put("modifiedDate", customer.modifiedDate)
            cObj.put("isActive", customer.isActive)
            custArr.put(cObj)
        }
        root.put("customers", custArr)

        // 3. Master Data
        val masterDataList = repository.allMasterData.first()
        val mastArr = JSONArray()
        masterDataList.forEach { master ->
            val mObj = JSONObject()
            mObj.put("type", master.type)
            mObj.put("value", master.value)
            mObj.put("extra", master.extra)
            mastArr.put(mObj)
        }
        root.put("master_data", mastArr)

        // 3.1 Masters (MasterEntity)
        val mastersList = db.masterDao().getAllMastersDirect()
        val mastersArr = JSONArray()
        mastersList.forEach { m ->
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("masterType", m.masterType)
            mObj.put("name", m.name)
            mObj.put("description", m.description)
            mObj.put("displayOrder", m.displayOrder)
            mObj.put("isActive", m.isActive)
            mObj.put("isDeleted", m.isDeleted)
            mObj.put("createdDate", m.createdDate)
            mObj.put("modifiedDate", m.modifiedDate)
            mastersArr.put(mObj)
        }
        root.put("masters_entities", mastersArr)

        // 4. Quotation Templates
        val templates = repository.allTemplates.first()
        val tempArr = JSONArray()
        templates.forEach { temp ->
            val tObj = JSONObject()
            tObj.put("name", temp.name)
            tObj.put("projectType", temp.projectType)
            tObj.put("category", temp.category)
            tObj.put("material", temp.material)
            tObj.put("finish", temp.finish)
            tObj.put("description", temp.description)
            tObj.put("itemsJson", temp.itemsJson)
            tempArr.put(tObj)
        }
        root.put("quotation_templates", tempArr)

        // 5. Quotations and Items
        val quotations = repository.allQuotations.first()
        val quoteArr = JSONArray()
        quotations.forEach { q ->
            val qObj = JSONObject()
            qObj.put("id", q.id) // Needed to relate with items
            qObj.put("quotationNumber", q.quotationNumber)
            qObj.put("date", q.date)
            qObj.put("customerId", q.customerId)
            qObj.put("customerName", q.customerName)
            qObj.put("customerPhone", q.customerPhone)
            qObj.put("customerAddress", q.customerAddress)
            qObj.put("projectType", q.projectType)
            qObj.put("category", q.category)
            qObj.put("material", q.material)
            qObj.put("finish", q.finish)
            qObj.put("subtotal", q.subtotal)
            qObj.put("discount", q.discount)
            qObj.put("gstRate", q.gstRate)
            qObj.put("gstAmount", q.gstAmount)
            qObj.put("grandTotal", q.grandTotal)
            qObj.put("termsAndConditions", q.termsAndConditions)
            qObj.put("warranty", q.warranty)
            qObj.put("status", q.status)

            val items = repository.getQuotationItemsDirect(q.id)
            val itemsArr = JSONArray()
            items.forEach { item ->
                val iObj = JSONObject()
                iObj.put("itemName", item.itemName)
                iObj.put("description", item.description)
                iObj.put("material", item.material)
                iObj.put("finish", item.finish)
                iObj.put("quantity", item.quantity)
                iObj.put("unit", item.unit)
                iObj.put("rate", item.rate)
                iObj.put("amount", item.amount)
                itemsArr.put(iObj)
            }
            qObj.put("items", itemsArr)
            quoteArr.put(qObj)
        }
        root.put("quotations", quoteArr)

        val rawJson = root.toString(2)
        return encryptAES(rawJson, password)
    }

    fun validateBackup(jsonStr: String, password: String = ""): Boolean {
        return try {
            val decryptedJson = if (jsonStr.trim().startsWith("{")) {
                jsonStr
            } else {
                decryptAES(jsonStr, password)
            }
            val root = JSONObject(decryptedJson)
            root.has("backup_version")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importBackup(db: AppDatabase, repository: QuotesRepository, jsonStr: String, password: String = ""): Boolean {
        return try {
            val decryptedJson = if (jsonStr.trim().startsWith("{")) {
                // If it starts with {, it is a legacy plaintext JSON backup
                jsonStr
            } else {
                // Otherwise it is an AES encrypted base64 backup
                decryptAES(jsonStr, password)
            }

            val root = JSONObject(decryptedJson)
            val version = root.optInt("backup_version", 1)
            if (version > 1) return false // Unsupported future version

            db.withTransaction {
                // Clear existing data (except default masters if desired, but we can do a total override for clean restore)
                db.clearAllTables()

                // 1. Restore Company Profile
                if (root.has("company_profile")) {
                    val coObj = root.getJSONObject("company_profile")
                    val profile = CompanyProfile(
                        id = 1,
                        companyName = coObj.optString("companyName"),
                        contactPerson = coObj.optString("contactPerson"),
                        ownerName = coObj.optString("ownerName"),
                        phone = coObj.optString("phone"),
                        whatsappNumber = coObj.optString("whatsappNumber"),
                        email = coObj.optString("email"),
                        website = coObj.optString("website"),
                        gstin = coObj.optString("gstin"),
                        address = coObj.optString("address"),
                        city = coObj.optString("city"),
                        district = coObj.optString("district"),
                        state = coObj.optString("state"),
                        pincode = coObj.optString("pincode"),
                        bankName = coObj.optString("bankName"),
                        accountHolderName = coObj.optString("accountHolderName"),
                        accountNumber = coObj.optString("accountNumber"),
                        ifsc = coObj.optString("ifsc"),
                        branch = coObj.optString("branch"),
                        upiId = coObj.optString("upiId"),
                        logoPath = coObj.optString("logoPath"),
                        signaturePath = coObj.optString("signaturePath"),
                        signatureText = coObj.optString("signatureText"),
                        tagline = coObj.optString("tagline", ""),
                        companySealPath = coObj.optString("companySealPath", ""),
                        defaultGstRate = coObj.optDouble("defaultGstRate", 18.0),
                        defaultDiscount = coObj.optDouble("defaultDiscount", 0.0),
                        defaultValidityDays = coObj.optInt("defaultValidityDays", 30),
                        defaultDeliveryDays = coObj.optInt("defaultDeliveryDays", 15),
                        termsAndConditions = coObj.optString("termsAndConditions", "")
                    )
                    db.companyProfileDao().insertOrUpdate(profile)
                }

                // 2. Restore Customers
                if (root.has("customers")) {
                    val custArr = root.getJSONArray("customers")
                    for (i in 0 until custArr.length()) {
                        val cObj = custArr.getJSONObject(i)
                        val name = if (cObj.has("customerName")) cObj.getString("customerName") else cObj.getString("name")
                        val phone = if (cObj.has("mobileNumber")) cObj.getString("mobileNumber") else cObj.getString("phone")
                        val originalId = cObj.optLong("customerId", 0)
                        repository.saveCustomer(
                            CustomerEntity(
                                customerId = originalId,
                                customerName = name,
                                mobileNumber = phone,
                                whatsappNumber = cObj.optString("whatsappNumber", ""),
                                email = cObj.optString("email", ""),
                                address = cObj.optString("address", ""),
                                siteLocation = cObj.optString("siteLocation", cObj.optString("companyName", "")),
                                city = cObj.optString("city", ""),
                                district = cObj.optString("district", ""),
                                state = cObj.optString("state", ""),
                                pincode = cObj.optString("pincode", ""),
                                notes = cObj.optString("notes", ""),
                                createdDate = cObj.optLong("createdDate", System.currentTimeMillis()),
                                modifiedDate = cObj.optLong("modifiedDate", System.currentTimeMillis()),
                                isActive = cObj.optBoolean("isActive", true)
                            )
                        )
                    }
                }

                // 3. Restore Master Data
                if (root.has("master_data")) {
                    val mastArr = root.getJSONArray("master_data")
                    val masters = mutableListOf<MasterData>()
                    for (i in 0 until mastArr.length()) {
                        val mObj = mastArr.getJSONObject(i)
                        masters.add(
                            MasterData(
                                type = mObj.getString("type"),
                                value = mObj.getString("value"),
                                extra = mObj.optString("extra")
                            )
                        )
                    }
                    db.masterDataDao().insertAll(masters)
                }

                // 3.1 Restore Masters Entities
                if (root.has("masters_entities")) {
                    val mArr = root.getJSONArray("masters_entities")
                    for (i in 0 until mArr.length()) {
                        val mObj = mArr.getJSONObject(i)
                        val m = MasterEntity(
                            id = mObj.optLong("id", 0),
                            masterType = mObj.getString("masterType"),
                            name = mObj.getString("name"),
                            description = mObj.optString("description", ""),
                            displayOrder = mObj.optInt("displayOrder", 0),
                            isActive = mObj.optBoolean("isActive", true),
                            isDeleted = mObj.optBoolean("isDeleted", false),
                            createdDate = mObj.optLong("createdDate", System.currentTimeMillis()),
                            modifiedDate = mObj.optLong("modifiedDate", System.currentTimeMillis())
                        )
                        db.masterDao().insertMaster(m)
                    }
                }

                // 4. Restore Templates
                if (root.has("quotation_templates")) {
                    val tempArr = root.getJSONArray("quotation_templates")
                    for (i in 0 until tempArr.length()) {
                        val tObj = tempArr.getJSONObject(i)
                        repository.saveTemplate(
                            QuotationTemplate(
                                name = tObj.getString("name"),
                                projectType = tObj.optString("projectType"),
                                category = tObj.optString("category"),
                                material = tObj.optString("material"),
                                finish = tObj.optString("finish"),
                                description = tObj.optString("description"),
                                itemsJson = tObj.optString("itemsJson")
                            )
                        )
                    }
                }

                // 5. Restore Quotations and Items
                if (root.has("quotations")) {
                    val quoteArr = root.getJSONArray("quotations")
                    for (i in 0 until quoteArr.length()) {
                        val qObj = quoteArr.getJSONObject(i)
                        
                        val originalQId = qObj.optInt("id", 0)
                        val q = Quotation(
                            id = originalQId,
                            quotationNumber = qObj.getString("quotationNumber"),
                            date = qObj.getLong("date"),
                            customerId = qObj.getInt("customerId"),
                            customerName = qObj.getString("customerName"),
                            customerPhone = qObj.optString("customerPhone"),
                            customerAddress = qObj.optString("customerAddress"),
                            projectType = qObj.optString("projectType"),
                            category = qObj.optString("category"),
                            material = qObj.optString("material"),
                            finish = qObj.optString("finish"),
                            subtotal = qObj.getDouble("subtotal"),
                            discount = qObj.optDouble("discount", 0.0),
                            gstRate = qObj.optDouble("gstRate", 18.0),
                            gstAmount = qObj.optDouble("gstAmount", 0.0),
                            grandTotal = qObj.getDouble("grandTotal"),
                            termsAndConditions = qObj.optString("termsAndConditions"),
                            warranty = qObj.optString("warranty"),
                            status = qObj.optString("status", "DRAFT")
                        )

                        // Insert quotation (will use its own ID if set)
                        val qId = db.quotationDao().insertQuotation(q).toInt()

                        if (qObj.has("items")) {
                            val itemsArr = qObj.getJSONArray("items")
                            val qItems = mutableListOf<QuotationItem>()
                            for (j in 0 until itemsArr.length()) {
                                val iObj = itemsArr.getJSONObject(j)
                                qItems.add(
                                    QuotationItem(
                                        quotationId = qId,
                                        itemName = if (iObj.has("itemName")) iObj.getString("itemName") else "",
                                        description = iObj.getString("description"),
                                        material = if (iObj.has("material")) iObj.getString("material") else "",
                                        finish = if (iObj.has("finish")) iObj.getString("finish") else "",
                                        quantity = iObj.getDouble("quantity"),
                                        unit = iObj.getString("unit"),
                                        rate = iObj.getDouble("rate"),
                                        amount = iObj.getDouble("amount")
                                    )
                                )
                            }
                            db.quotationItemDao().insertAll(qItems)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
