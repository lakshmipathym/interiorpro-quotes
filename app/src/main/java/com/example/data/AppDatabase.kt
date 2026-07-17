package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CompanyProfile::class,
        CustomerEntity::class,
        MasterEntity::class,
        MasterData::class,
        QuotationTemplate::class,
        Quotation::class,
        QuotationItem::class,
        Client::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun masterDao(): MasterDao
    abstract fun masterDataDao(): MasterDataDao
    abstract fun quotationTemplateDao(): QuotationTemplateDao
    abstract fun quotationDao(): QuotationDao
    abstract fun quotationItemDao(): QuotationItemDao
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "interior_pro_quotes_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                
                // Seed Company Profile (Default empty, ready to edit)
                db.execSQL(
                    """
                    INSERT INTO company_profile (
                        id, companyName, contactPerson, ownerName, phone, whatsappNumber, email, website, gstin,
                        address, city, district, state, pincode,
                        bankName, accountHolderName, accountNumber, ifsc, branch,
                        upiId, logoPath, signaturePath, signatureText,
                        tagline, companySealPath, defaultGstRate, defaultDiscount, defaultValidityDays, defaultDeliveryDays, termsAndConditions,
                        defaultWarranty, defaultDeliveryTime, defaultInstallationTime, defaultPaymentTerms, defaultQuoteValidity, additionalConditions
                    ) VALUES (
                        1, 'Your Business Name', 'Owner Name', 'Owner Name', '+91 98765 43210', '+91 98765 43210', 'owner@example.com', 'www.example.com', '',
                        '123, Design Studio, Main Road', 'City Name', 'District Name', 'State Name', '123456',
                        'State Bank of India', 'Owner Name', '1234567890', 'SBIN0001234', 'Main Branch',
                        'owner@upi', '', '', 'For Your Business Name',
                        'Premium Interior Designing Solutions', '', 18.0, 0.0, 30, 15, '50% Advance, 40% on Material Delivery, 10% after Installation.',
                        '1 Year Warranty', '15 Days', '7 Days', '50% Advance, 40% on Material Delivery, 10% after Installation.', '30 Days', 'Electrical, Plumbing, and Civil works to be arranged by the client.'
                    )
                    """.trimIndent()
                )

                // Seed Project Types
                val projectTypes = listOf(
                    "Modular Kitchen", "Wardrobe", "Living Room TV Unit", 
                    "Aluminium Partition", "Full Home Interior", "Office Workstations"
                )
                projectTypes.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('PROJECT_TYPE', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('PROJECT_TYPE', '$value', 'Standard project type for quotations', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Categories
                val categories = listOf(
                    "Base Cabinets", "Wall Cabinets", "Tall Units", 
                    "Shutters", "Hardware & Accessories", "Countertop", "Glass Partition"
                )
                categories.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('CATEGORY', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('CATEGORY', '$value', 'Standard category category', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Materials
                val materials = listOf(
                    "BWP Plywood", "MDF (Exterior Grade)", "HDF", 
                    "Aluminium Section Framework", "Aluminium Composite Panel (ACP)", "Particle Board"
                )
                materials.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('MATERIAL', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('MATERIAL', '$value', 'Standard raw material', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Finish Types
                val finishes = listOf(
                    "High Gloss Laminate", "Matte Laminate", "Acrylic Finish", 
                    "PU Paint", "Powder Coated", "Anodized"
                )
                finishes.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('FINISH_TYPE', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('FINISH_TYPE', '$value', 'Standard surface finish', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Units
                val units = listOf("Sq.Ft", "Rft (Running Foot)", "Pcs", "Nos", "Meters")
                units.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('UNIT', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('UNIT', '$value', 'Standard measurement unit', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Warranty
                val warranties = listOf(
                    "No Warranty", "1 Year Warranty", "5 Years Warranty", 
                    "10 Years Warranty (BWP)", "Lifetime Warranty (Hardware)"
                )
                warranties.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('WARRANTY', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('WARRANTY', '$value', 'Standard warranty policy', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Terms & Conditions
                val terms = listOf(
                    "50% Advance, 40% on Material Delivery, 10% after Installation.",
                    "Quotation is valid for 30 days from date of issue.",
                    "Electrical, Plumbing, and Civil works to be arranged by the client.",
                    "Delivery within 15-20 business days after layout approval."
                )
                terms.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('TERMS', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('TERMS', '$value', 'Standard terms conditions template', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed Accessories
                val accessories = listOf(
                    "Soft-close Hinge", "Telescopic Drawer Slide", "Corner Carousel", 
                    "Pantry Pull-out", "Cutlery Tray", "Profile Handle"
                )
                accessories.forEachIndexed { idx, value ->
                    db.execSQL("INSERT INTO master_data (type, value, extra) VALUES ('ACCESSORY', '$value', '')")
                    db.execSQL("INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate) VALUES ('ACCESSORY', '$value', 'Standard fixture accessory', ${idx * 10}, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})")
                }

                // Seed standard Quotation Templates
                val standardKitchenItemsJson = """
                    [
                      {"description":"Base Cabinets (BWP Plywood, Matte Laminate)","quantity":10.0,"unit":"Rft (Running Foot)","rate":1850.0},
                      {"description":"Wall Cabinets (Exterior MDF, Gloss Laminate)","quantity":8.0,"unit":"Rft (Running Foot)","rate":1350.0},
                      {"description":"Soft-close Drawer Runners (Standard)","quantity":4.0,"unit":"Pcs","rate":950.0},
                      {"description":"Pantry Pull-out System","quantity":1.0,"unit":"Pcs","rate":12500.0},
                      {"description":"Profile Handles (Anodized)","quantity":12.0,"unit":"Nos","rate":180.0}
                    ]
                """.trimIndent().replace("\n", "").replace(" ", "")

                db.execSQL(
                    """
                    INSERT INTO quotation_template (name, projectType, category, material, finish, description, itemsJson)
                    VALUES ('Standard Modular Kitchen', 'Modular Kitchen', 'Base Cabinets', 'BWP Plywood', 'Matte Laminate', 'Standard L-Shape modular kitchen package', '$standardKitchenItemsJson')
                    """.trimIndent()
                )

                val standardWardrobeItemsJson = """
                    [
                      {"description":"Wardrobe Carcass (Plywood, Matte Finish)","quantity":45.0,"unit":"Sq.Ft","rate":1100.0},
                      {"description":"Wardrobe Sliding Doors (Acrylic Finish)","quantity":24.0,"unit":"Sq.Ft","rate":1450.0},
                      {"description":"Soft-close Hettich Hinges","quantity":6.0,"unit":"Pcs","rate":450.0},
                      {"description":"Drawer Lock & Handle set","quantity":2.0,"unit":"Nos","rate":650.0}
                    ]
                """.trimIndent().replace("\n", "").replace(" ", "")

                db.execSQL(
                    """
                    INSERT INTO quotation_template (name, projectType, category, material, finish, description, itemsJson)
                    VALUES ('Premium Sliding Wardrobe', 'Wardrobe', 'Shutters', 'BWP Plywood', 'Acrylic Finish', 'Premium wardrobe with soft-close sliding doors', '$standardWardrobeItemsJson')
                    """.trimIndent()
                )
            }
        }
    }
}
