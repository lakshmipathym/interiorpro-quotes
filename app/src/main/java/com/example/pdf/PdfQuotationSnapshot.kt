package com.example.pdf

data class PdfQuotationSnapshot(
    val company: CompanySnapshot,
    val customer: CustomerSnapshot,
    val quotation: QuotationSnapshot,
    val items: List<ItemSnapshot>,
    val financial: FinancialSnapshot,
    val payment: PaymentSnapshot,
    val terms: List<TermsSnapshot>,
    val signature: SignatureSnapshot
)

data class CompanySnapshot(
    val canonicalName: String, // One canonical name
    val address: String,
    val contact: String,
    val email: String,
    val gstin: String,
    val website: String,
    val logoUri: String?,
    val sealUri: String?
)

data class CustomerSnapshot(
    val name: String,
    val mobile: String,
    val email: String,
    val address: String,
    val city: String,
    val state: String,
    val pincode: String,
    val siteAddress: String,
    val gstin: String
)

data class QuotationSnapshot(
    val quotationNumber: String,
    val date: String,
    val validUntil: String,
    val referenceInfo: String
)

data class ItemSnapshot(
    val slNo: Int,
    val itemName: String,
    val itemDescription: String,
    val specifications: List<Pair<String, String>>, // material, finish, etc.
    val dimensions: List<String>, // e.g. ["W : 10'", "H : 12'", "D : 1' 6""]
    val billableLabel: String, // "Area : 120.00 Sq.Ft" or "Volume : ..." etc.
    val quantityCount: Double,
    val quantityDisplay: String, // "120.00 Sq.Ft"
    val rateDisplay: String, // "₹ 1,500 / Sq.Ft"
    val amountDisplay: String, // "₹ 1,80,000"
    val isHeader: Boolean
)

data class FinancialSnapshot(
    val subtotalDisplay: String,
    val discountDisplay: String?,
    val gstDisplay: String?,
    val transportDisplay: String?,
    val installationDisplay: String?,
    val extraChargesDisplay: String?,
    val roundOffDisplay: String?,
    val grandTotalValue: Double, // For amount in words
    val grandTotalDisplay: String
)

data class PaymentSnapshot(
    val upiId: String,
    val accountName: String,
    val bankName: String,
    val accountNumber: String,
    val ifscCode: String,
    val branchName: String
)

data class TermsSnapshot(
    val number: Int,
    val label: String,
    val value: String
)

data class SignatureSnapshot(
    val ownerName: String,
    val designation: String,
    val signatureUri: String?
)
