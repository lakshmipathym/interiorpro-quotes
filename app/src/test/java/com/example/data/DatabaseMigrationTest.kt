package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration_test_db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate16To17_preservesDataAndAddsDefaultColumns() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `company_profile` (`id` INTEGER NOT NULL, `companyName` TEXT NOT NULL, `ownerName` TEXT NOT NULL, `contactPerson` TEXT NOT NULL, `phone` TEXT NOT NULL, `whatsappNumber` TEXT NOT NULL, `email` TEXT NOT NULL, `website` TEXT NOT NULL, `address` TEXT NOT NULL, `city` TEXT NOT NULL, `district` TEXT NOT NULL, `state` TEXT NOT NULL, `pincode` TEXT NOT NULL, `bankName` TEXT NOT NULL, `accountHolderName` TEXT NOT NULL, `accountNumber` TEXT NOT NULL, `ifsc` TEXT NOT NULL, `branch` TEXT NOT NULL, `upiId` TEXT NOT NULL, `logoPath` TEXT NOT NULL, `gstin` TEXT NOT NULL, `signatureText` TEXT NOT NULL, `signaturePath` TEXT NOT NULL, `tagline` TEXT NOT NULL, `companySealPath` TEXT NOT NULL, `brandColor` TEXT NOT NULL, `defaultGstRate` REAL NOT NULL, `defaultDiscount` REAL NOT NULL, `defaultValidityDays` INTEGER NOT NULL, `defaultDeliveryDays` INTEGER NOT NULL, `termsAndConditions` TEXT NOT NULL, `defaultWarranty` TEXT NOT NULL, `defaultDeliveryTime` TEXT NOT NULL, `defaultInstallationTime` TEXT NOT NULL, `defaultPaymentTerms` TEXT NOT NULL, `defaultQuoteValidity` TEXT NOT NULL, `additionalConditions` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `customer` (`customerId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerName` TEXT NOT NULL, `mobileNumber` TEXT NOT NULL, `whatsappNumber` TEXT NOT NULL, `email` TEXT NOT NULL, `address` TEXT NOT NULL, `siteLocation` TEXT NOT NULL, `city` TEXT NOT NULL, `district` TEXT NOT NULL, `state` TEXT NOT NULL, `pincode` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdDate` INTEGER NOT NULL, `modifiedDate` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `companyName` TEXT NOT NULL, `contactPerson` TEXT NOT NULL, `gstin` TEXT NOT NULL, `siteAddress` TEXT NOT NULL, `country` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `masters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `masterType` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `displayOrder` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `createdDate` INTEGER NOT NULL, `modifiedDate` INTEGER NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `quotation_template` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `projectType` TEXT NOT NULL, `category` TEXT NOT NULL, `material` TEXT NOT NULL, `finish` TEXT NOT NULL, `rawWidth` TEXT NOT NULL, `rawHeight` TEXT NOT NULL, `rawDepth` TEXT NOT NULL, `parsedWidth` REAL NOT NULL, `parsedHeight` REAL NOT NULL, `parsedDepth` REAL NOT NULL, `rawQuantity` REAL NOT NULL, `billableQuantity` REAL NOT NULL, `description` TEXT NOT NULL, `itemsJson` TEXT NOT NULL)")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `quotation_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quotationId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `description` TEXT NOT NULL, `material` TEXT NOT NULL, `finish` TEXT NOT NULL, `rawWidth` TEXT NOT NULL, `rawHeight` TEXT NOT NULL, `rawDepth` TEXT NOT NULL, `parsedWidth` REAL NOT NULL, `parsedHeight` REAL NOT NULL, `parsedDepth` REAL NOT NULL, `rawQuantity` REAL NOT NULL, `billableQuantity` REAL NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `rate` REAL NOT NULL, `amount` REAL NOT NULL)")
                    
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `quotation` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `quotationNumber` TEXT NOT NULL, `date` INTEGER NOT NULL, `customerId` INTEGER NOT NULL, `customerName` TEXT NOT NULL, `customerPhone` TEXT NOT NULL, `customerAddress` TEXT NOT NULL, `siteName` TEXT NOT NULL, `siteAddress` TEXT NOT NULL, `projectName` TEXT NOT NULL, `projectType` TEXT NOT NULL, `category` TEXT NOT NULL, `material` TEXT NOT NULL, `finish` TEXT NOT NULL, `rawWidth` TEXT NOT NULL, `rawHeight` TEXT NOT NULL, `rawDepth` TEXT NOT NULL, `parsedWidth` REAL NOT NULL, `parsedHeight` REAL NOT NULL, `parsedDepth` REAL NOT NULL, `rawQuantity` REAL NOT NULL, `billableQuantity` REAL NOT NULL, `subtotal` REAL NOT NULL, `discount` REAL NOT NULL, `gstRate` REAL NOT NULL, `gstAmount` REAL NOT NULL, `transport` REAL NOT NULL, `installation` REAL NOT NULL, `extraCharges` REAL NOT NULL, `roundOff` REAL NOT NULL, `grandTotal` REAL NOT NULL, `advance` REAL NOT NULL, `balance` REAL NOT NULL, `taxableAmount` REAL NOT NULL, `amountInWords` TEXT NOT NULL, `companyNameSnapshot` TEXT NOT NULL, `companyOwnerNameSnapshot` TEXT NOT NULL, `companyPhoneSnapshot` TEXT NOT NULL, `companyEmailSnapshot` TEXT NOT NULL, `companyAddressSnapshot` TEXT NOT NULL, `companyGstinSnapshot` TEXT NOT NULL, `companyBankNameSnapshot` TEXT NOT NULL, `companyAccountNameSnapshot` TEXT NOT NULL, `companyAccountNumberSnapshot` TEXT NOT NULL, `companyIfscSnapshot` TEXT NOT NULL, `companyBranchSnapshot` TEXT NOT NULL, `companyUpiIdSnapshot` TEXT NOT NULL, `termsAndConditions` TEXT NOT NULL, `warranty` TEXT NOT NULL, `customerNotes` TEXT NOT NULL, `internalNotes` TEXT NOT NULL, `validityDays` INTEGER NOT NULL, `status` TEXT NOT NULL
                        )
                    """.trimIndent())

                    // Create indices
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_customerName` ON `customer` (`customerName`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_mobileNumber` ON `customer` (`mobileNumber`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_email` ON `customer` (`email`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_createdDate` ON `customer` (`createdDate`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_masters_masterType` ON `masters` (`masterType`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_masters_name` ON `masters` (`name`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_masters_isDeleted` ON `masters` (`isDeleted`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_masters_masterType_isDeleted_displayOrder` ON `masters` (`masterType`, `isDeleted`, `displayOrder`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_quotation_item_quotationId` ON `quotation_item` (`quotationId`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val factory = FrameworkSQLiteOpenHelperFactory()
        val helper = factory.create(configuration)
        val db = helper.writableDatabase

        db.execSQL("""
            INSERT INTO quotation (
                quotationNumber, date, customerId, customerName, customerPhone, customerAddress, 
                siteName, siteAddress, projectName, projectType, category, material, finish, 
                rawWidth, rawHeight, rawDepth, parsedWidth, parsedHeight, parsedDepth, 
                rawQuantity, billableQuantity, subtotal, discount, gstRate, gstAmount, 
                transport, installation, extraCharges, roundOff, grandTotal, advance, balance, 
                taxableAmount, amountInWords, companyNameSnapshot, companyOwnerNameSnapshot, 
                companyPhoneSnapshot, companyEmailSnapshot, companyAddressSnapshot, 
                companyGstinSnapshot, companyBankNameSnapshot, companyAccountNameSnapshot, 
                companyAccountNumberSnapshot, companyIfscSnapshot, companyBranchSnapshot, 
                companyUpiIdSnapshot, termsAndConditions, warranty, customerNotes, internalNotes, 
                validityDays, status
            ) VALUES (
                'QT-123', 0, 1, 'John Doe', '1234567890', '123 Main St', 
                'Site A', 'Site A Addr', 'Proj A', 'Type A', 'Cat A', 'Mat A', 'Fin A', 
                '10', '10', '10', 10.0, 10.0, 10.0, 
                1.0, 1.0, 100.0, 0.0, 18.0, 18.0, 
                0.0, 0.0, 0.0, 0.0, 118.0, 0.0, 118.0, 
                100.0, 'One Hundred', 'My Company', 'Me', 
                '0987654321', 'me@company.com', 'Company Addr', 
                'GST123', 'Bank A', 'Acc Me', 
                '12345', 'IFSC001', 'Branch 1', 
                'me@upi', 'Terms', 'Warranty', 'Cust Notes', 'Int Notes', 
                30, 'FINALIZED'
            )
        """.trimIndent())

        db.close()
        helper.close()

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_16_17)
            .allowMainThreadQueries()
            .build()

        val cursor = roomDb.query("SELECT * FROM quotation", null)
        assertTrue("Expected at least one row", cursor.moveToFirst())

        assertEquals("QT-123", cursor.getString(cursor.getColumnIndexOrThrow("quotationNumber")))
        assertEquals("John Doe", cursor.getString(cursor.getColumnIndexOrThrow("customerName")))

        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("customerEmail")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("customerWhatsapp")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("customerContactPerson")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("customerCompanyName")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("customerGstin")))

        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("companyWebsiteSnapshot")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("companyWhatsappSnapshot")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("companyLogoPathSnapshot")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("companySignaturePathSnapshot")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("companySealPathSnapshot")))

        cursor.close()
        roomDb.close()
    }
}
