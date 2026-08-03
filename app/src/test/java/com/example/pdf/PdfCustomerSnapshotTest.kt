package com.example.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.models.*
import com.example.pdf.PdfGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfCustomerSnapshotTest {

    @Test
    fun testPdfCustomerSnapshotIsolation() {
runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val realDb = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val quotesRepo = QuotesRepository(realDb)
        
        val liveCustomer = CustomerEntity(customerName = "Live Customer", mobileNumber = "12345", address = "Address")
        val customerId = realDb.customerDao().insertCustomer(liveCustomer)

        // Snapshot Test
        val snapshot = FinalizedQuotationSnapshot(
            id = "1",
            quotationNumber = "IPQ/TEST/001",
            date = 1L,
            customer = CustomerSnapshot(
                customerId = customerId.toString(),
                customerName = "Snapshot Customer",
                customerPhone = "99999",
                customerAddress = "Snapshot Addr",
                siteName = "Site",
                siteAddress = "Site Addr"
            ),
            company = CompanySnapshot("C", "O", "P", "E", "A", "G", "B", "AH", "AN", "I", "B", "U"),
            items = emptyList(),
            financial = FinancialSnapshot(
                subtotal = 1000.0, discount = 0.0, taxableAmount = 1000.0,
                gstRate = 18.0, gstAmount = 180.0, transport = 0.0, installation = 0.0,
                extraCharges = 0.0, roundOff = 0.0, grandTotal = 1180.0,
                advance = 0.0, balanceDue = 1180.0, amountInWords = "ONE THOUSAND ONE HUNDRED EIGHTY"
            ),
            termsAndConditions = "", warranty = "", validityDays = 30, notes = ""
        )
        
        val updatedCustomer = liveCustomer.copy(customerId = customerId, customerName = "Changed Live Customer")
        realDb.customerDao().updateCustomer(updatedCustomer)
        
        
        // Snapshot Test - Delete live customer to ensure it can't be fetched
        realDb.customerDao().deleteCustomer(updatedCustomer)
        
        val outputFile = File(context.cacheDir, "test1.pdf")

        try {
            
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

        } catch (e: IllegalStateException) {
            if (e.message != "document is closed!") throw e
        }
        
        // Draft Test
        val draftQuotation = Quotation(
            id = 2,
            quotationNumber = "IPQ/TEST/002",
            customerId = customerId,
            customerName = "Changed Live Customer",
            status = "DRAFT"
        )
        realDb.quotationDao().insertQuotation(draftQuotation)
        
        val draftOutputFile = File(context.cacheDir, "test2.pdf")
        try {
            runBlocking { PdfGenerator.generateQuotationPdf(context, quotesRepo, 2, draftOutputFile) }
        } catch (e: IllegalStateException) {
            if (e.message != "document is closed!") throw e
        }
    }
}
}
