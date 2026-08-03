package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "company_profile")
data class CompanyProfile(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "",
    val ownerName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pincode: String = "",
    val bankName: String = "",
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val branch: String = "",
    val upiId: String = "",
    val logoPath: String = "",
    val gstin: String = "",
    val signatureText: String = "",
    val signaturePath: String = "",
    val tagline: String = "",
    val companySealPath: String = "",
    val brandColor: String = "",
    val defaultGstRate: Double = 0.0,
    val defaultDiscount: Double = 0.0,
    val defaultValidityDays: Int = 30,
    val defaultDeliveryDays: Int = 15,
    val termsAndConditions: String = "",
    val defaultWarranty: String = "",
    val defaultDeliveryTime: String = "",
    val defaultInstallationTime: String = "",
    val defaultPaymentTerms: String = "",
    val defaultQuoteValidity: String = "",
    val additionalConditions: String = ""
)

@Entity(
    tableName = "customer",
    indices = [
        Index("customerName"),
        Index("mobileNumber"),
        Index("email"),
        Index("createdDate")
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val customerId: Long = 0,
    val customerName: String = "",
    val mobileNumber: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val address: String = "",
    val siteLocation: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pincode: String = "",
    val notes: String = "",
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    
    // ERP Standard Fields
    val companyName: String = "",
    val contactPerson: String = "",
    val gstin: String = "",
    val siteAddress: String = "",
    val country: String = "India"
)

@Entity(
    tableName = "masters",
    indices = [
        Index("masterType"),
        Index("name"),
        Index("isDeleted"),
        Index(value = ["masterType", "isDeleted", "displayOrder"])
    ]
)
data class MasterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val masterType: String,
    val name: String,
    val description: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "quotation_template")
data class QuotationTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val projectType: String = "",
    val category: String = "",
    val material: String = "",
    val finish: String = "",
    val rawWidth: String = "",
    val rawHeight: String = "",
    val rawDepth: String = "",
    val parsedWidth: Double = 0.0,
    val parsedHeight: Double = 0.0,
    val parsedDepth: Double = 0.0,
    val rawQuantity: Double = 0.0,
    val billableQuantity: Double = 0.0,
    val description: String = "",
    val itemsJson: String = "" // Stores JSON array of TemplateItem
)

data class TemplateItem(
    val description: String,
    val quantity: Double,
    val unit: String,
    val rate: Double
)

@Entity(tableName = "quotation")
data class Quotation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quotationNumber: String = "",
    val date: Long = 0L,
    val customerId: Long = 0L,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val siteName: String = "",
    val siteAddress: String = "",
    val customerEmail: String = "",
    val customerWhatsapp: String = "",
    val customerContactPerson: String = "",
    val customerCompanyName: String = "",
    val customerGstin: String = "",
    val projectName: String = "",
    val projectType: String = "",
    val category: String = "",
    val material: String = "",
    val finish: String = "",
    val rawWidth: String = "",
    val rawHeight: String = "",
    val rawDepth: String = "",
    val parsedWidth: Double = 0.0,
    val parsedHeight: Double = 0.0,
    val parsedDepth: Double = 0.0,
    val rawQuantity: Double = 0.0,
    val billableQuantity: Double = 0.0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gstRate: Double = 0.0,
    val gstAmount: Double = 0.0,
    val transport: Double = 0.0,
    val installation: Double = 0.0,
    val extraCharges: Double = 0.0,
    val roundOff: Double = 0.0,
    val grandTotal: Double = 0.0,
    val advance: Double = 0.0,
    val balance: Double = 0.0,
    val taxableAmount: Double = 0.0,
    val amountInWords: String = "",
    val companyNameSnapshot: String = "",
    val companyOwnerNameSnapshot: String = "",
    val companyPhoneSnapshot: String = "",
    val companyEmailSnapshot: String = "",
    val companyAddressSnapshot: String = "",
    val companyGstinSnapshot: String = "",
    val companyBankNameSnapshot: String = "",
    val companyAccountNameSnapshot: String = "",
    val companyAccountNumberSnapshot: String = "",
    val companyIfscSnapshot: String = "",
    val companyBranchSnapshot: String = "",
    val companyUpiIdSnapshot: String = "",
    val companyWebsiteSnapshot: String = "",
    val companyWhatsappSnapshot: String = "",
    val companyLogoPathSnapshot: String = "",
    val companySignaturePathSnapshot: String = "",
    val companySealPathSnapshot: String = "",
    val termsAndConditions: String = "",
    val warranty: String = "",
    val customerNotes: String = "",
    val internalNotes: String = "",
    val validityDays: Int = 0,
    val deliveryTime: String = "",
    val installationTime: String = "",
    val paymentTerms: String = "",
    val additionalConditions: String = "",
    val status: String = "DRAFT"
)

@Entity(
    tableName = "quotation_item",
    indices = [Index("quotationId")]
)
data class QuotationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quotationId: Int,
    val itemName: String = "",
    val description: String = "",
    val material: String = "",
    val finish: String = "",
    val rawWidth: String = "",
    val rawHeight: String = "",
    val rawDepth: String = "",
    val parsedWidth: Double = 0.0,
    val parsedHeight: Double = 0.0,
    val parsedDepth: Double = 0.0,
    val rawQuantity: Double = 0.0,
    val billableQuantity: Double = 0.0,
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val amount: Double
)
