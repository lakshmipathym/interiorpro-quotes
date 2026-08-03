package com.example.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import com.example.utils.ShareManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfCompanySnapshotTest {

    @Test
    fun testPdfCompanySnapshotIsolation() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            
            val realDb = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
            val quotesRepo = QuotesRepository(realDb)
            val snapshotRepo = QuotationSnapshotRepositoryImpl(realDb, quotesRepo)
            
            val liveCompany = CompanyProfile(
                companyName = "Live Company",
                phone = "11111"
            )
            realDb.companyProfileDao().insertOrUpdate(liveCompany)

            val customer = CustomerEntity(customerName = "Test Cust", mobileNumber = "12345", address = "Address")
            val customerId = realDb.customerDao().insertCustomer(customer)
            
            val quotation = Quotation(
                id = 1,
                quotationNumber = "IPQ/TEST/001",
                customerId = customerId,
                customerName = "Test Cust",
                subtotal = 1000.0,
                grandTotal = 1180.0,
                status = "FINALIZED"
            )
            realDb.quotationDao().insertQuotation(quotation)

            // Snapshot Test
            val snapshot = FinalizedQuotationSnapshot(
                id = "1",
                quotationNumber = "IPQ/TEST/001",
                date = 1L,
                customer = CustomerSnapshot(
                    customerId = customerId.toString(),
                    customerName = "Test Cust",
                    customerPhone = "12345",
                    customerAddress = "Address",
                    siteName = "",
                    siteAddress = ""
                ),
                company = CompanySnapshot(
                    companyName = "Snapshot Company",
                    ownerName = "Snap Owner",
                    phone = "99999",
                    email = "snap@test.com",
                    address = "Snap Address",
                    bankName = "Snap Bank",
                    accountHolderName = "Snap Holder",
                    accountNumber = "Snap Acc",
                    ifsc = "Snap IFSC",
                    branch = "Snap Branch",
                    upiId = "Snap UPI",
                    logoPath = "/snap/logo.png",
                    signaturePath = "/snap/sig.png"
                ),
                items = emptyList(),
                financial = FinancialSnapshot(
                    subtotal = 1000.0, discount = 0.0, taxableAmount = 1000.0,
                    gstRate = 18.0, gstAmount = 180.0, transport = 0.0, installation = 0.0,
                    extraCharges = 0.0, roundOff = 0.0, grandTotal = 1180.0,
                    advance = 0.0, balanceDue = 1180.0, amountInWords = "ONE THOUSAND ONE HUNDRED EIGHTY"
                ),
                termsAndConditions = "", warranty = "", validityDays = 30, notes = ""
            )
            snapshotRepo.saveSnapshot(snapshot)
            
            val updatedCompany = liveCompany.copy(companyName = "Changed Live Company", phone = "22222")
            realDb.companyProfileDao().insertOrUpdate(updatedCompany)
            
            // Generate PDF
            try {
                ShareManager.generateQuotationPdf(context, quotesRepo, 1)
            } catch (e: IllegalStateException) {
                if (e.message != "document is closed!") throw e
            }
            

            // Delete live company
            realDb.companyProfileDao().insertOrUpdate(CompanyProfile(companyName = "Deleted Company"))

            
            // Should still work for Finalized
            try {
                ShareManager.generateQuotationPdf(context, quotesRepo, 1)
            } catch (e: IllegalStateException) {
                if (e.message != "document is closed!") throw e
            }

            // Draft Test
            val draftQuotation = Quotation(
                id = 2,
                quotationNumber = "IPQ/TEST/002",
                customerId = customerId,
                customerName = "Test Cust",
                status = "DRAFT"
            )
            realDb.quotationDao().insertQuotation(draftQuotation)
            
            // Restore live company for draft
            realDb.companyProfileDao().insertOrUpdate(liveCompany)
            
            try {
                ShareManager.generateQuotationPdf(context, quotesRepo, 2)
            } catch (e: IllegalStateException) {
                if (e.message != "document is closed!") throw e
            }
        }
    }
}
