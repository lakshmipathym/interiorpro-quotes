package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "company_profile")
data class CompanyProfile(
    @PrimaryKey val id: Int = 1, // Fixed ID to ensure only 1 profile exists
    val companyName: String = "",
    val contactPerson: String = "", // Kept for compatibility
    val ownerName: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val website: String = "",
    val gstin: String = "",
    
    // Address Details
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val pincode: String = "",
    
    // Bank Details
    val bankName: String = "",
    val accountHolderName: String = "",
    val accountNumber: String = "",
    val ifsc: String = "",
    val branch: String = "",
    
    // Payment Details
    val upiId: String = "",
    
    // Branding URIs
    val logoPath: String = "",
    val signaturePath: String = "",
    val signatureText: String = "",
    
    // Module 3 Additions
    val tagline: String = "",
    val companySealPath: String = "",
    val defaultGstRate: Double = 18.0,
    val defaultDiscount: Double = 0.0,
    val defaultValidityDays: Int = 30,
    val defaultDeliveryDays: Int = 15,
    val termsAndConditions: String = "",
    
    // customizable terms & conditions fields
    val defaultWarranty: String = "1 Year Warranty",
    val defaultDeliveryTime: String = "15 Days",
    val defaultInstallationTime: String = "7 Days",
    val defaultPaymentTerms: String = "50% Advance, 40% on Material Delivery, 10% after Installation.",
    val defaultQuoteValidity: String = "30 Days",
    val additionalConditions: String = "Electrical, Plumbing, and Civil works to be arranged by the client."
)

@Entity(
    tableName = "customer",
    indices = [Index("customerName"), Index("mobileNumber")]
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
        Index("name")
    ]
)
data class MasterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val masterType: String, // "PROJECT_TYPE", "CATEGORY", "MATERIAL", "FINISH_TYPE", "UNIT", "WARRANTY", "ACCESSORY", "TERMS"
    val name: String,
    val description: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false, // Soft delete flag
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "master_data",
    indices = [Index(value = ["type", "value"], unique = true)]
)
data class MasterData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "PROJECT_TYPE", "CATEGORY", "MATERIAL", "FINISH_TYPE", "UNIT", "WARRANTY", "TERMS", "ACCESSORY"
    val value: String,
    val extra: String = "" // Optional metadata like standard rate or description
)

@Entity(tableName = "quotation_template")
data class QuotationTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val projectType: String = "",
    val category: String = "",
    val material: String = "",
    val finish: String = "",
    val description: String = "",
    val itemsJson: String = "" // JSON-serialized preset items: List<TemplateItem>
)

@Entity(
    tableName = "quotation",
    indices = [
        Index(value = ["quotationNumber"], unique = true),
        Index("customerName"),
        Index("customerPhone"),
        Index("category"),
        Index("material")
    ]
)
data class Quotation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quotationNumber: String,
    val date: Long = System.currentTimeMillis(),
    val customerId: Int,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val projectType: String = "",
    val category: String = "",
    val material: String = "",
    val finish: String = "",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gstRate: Double = 18.0, // Default 18% GST
    val gstAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val termsAndConditions: String = "",
    val warranty: String = "",
    val status: String = "DRAFT" // "DRAFT", "SENT", "APPROVED", "REJECTED"
)

@Entity(
    tableName = "quotation_item",
    foreignKeys = [
        ForeignKey(
            entity = Quotation::class,
            parentColumns = ["id"],
            childColumns = ["quotationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quotationId")]
)
data class QuotationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quotationId: Int,
    val itemName: String = "",
    val description: String,
    val material: String = "",
    val finish: String = "",
    val quantity: Double,
    val unit: String,
    val rate: Double,
    val amount: Double
)

data class TemplateItem(
    val description: String,
    val quantity: Double,
    val unit: String,
    val rate: Double
)
