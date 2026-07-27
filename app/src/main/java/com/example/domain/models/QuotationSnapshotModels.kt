package com.example.domain.models

data class FinalizedQuotationSnapshot(
    val id: String,
    val quotationNumber: String,
    val date: Long,
    val customer: CustomerSnapshot,
    val company: CompanySnapshot,
    val items: List<FinalizedItemSnapshot>,
    val financial: FinancialSnapshot,
    val termsAndConditions: String,
    val warranty: String,
    val validityDays: Int,
    val notes: String
)

data class CustomerSnapshot(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val siteName: String,
    val siteAddress: String
)

data class CompanySnapshot(
    val companyName: String,
    val ownerName: String,
    val phone: String,
    val email: String,
    val address: String,
    val gstin: String,
    val bankName: String,
    val accountHolderName: String,
    val accountNumber: String,
    val ifsc: String,
    val branch: String,
    val upiId: String
)

data class FinalizedItemSnapshot(
    val itemId: String,
    val itemName: String,
    val description: String,
    val material: String,
    val finish: String,
    val rawWidth: String,
    val rawHeight: String,
    val rawDepth: String,
    val parsedWidth: Double,
    val parsedHeight: Double,
    val parsedDepth: Double,
    val parsedUnit: UnitType,
    val quantity: Double,
    val billableQuantity: Double,
    val rate: Double,
    val itemAmount: Double
)

data class FinancialSnapshot(
    val subtotal: Double,
    val discount: Double,
    val taxableAmount: Double,
    val gstRate: Double,
    val gstAmount: Double,
    val transport: Double,
    val installation: Double,
    val extraCharges: Double,
    val roundOff: Double,
    val grandTotal: Double,
    val advance: Double,
    val balanceDue: Double,
    val amountInWords: String
)
