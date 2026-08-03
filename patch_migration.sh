sed -i 's/version = 16/version = 17/' app/src/main/java/com/example/data/AppDatabase.kt

sed -i '/val MIGRATION_15_16/i \
        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {\
            override fun migrate(db: SupportSQLiteDatabase) {\
                db.execSQL("ALTER TABLE quotation ADD COLUMN customerEmail TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN customerWhatsapp TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN customerContactPerson TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN customerCompanyName TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN customerGstin TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyWebsiteSnapshot TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyWhatsappSnapshot TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyLogoPathSnapshot TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN companySignaturePathSnapshot TEXT NOT NULL DEFAULT '\'\'\''")\
                db.execSQL("ALTER TABLE quotation ADD COLUMN companySealPathSnapshot TEXT NOT NULL DEFAULT '\'\'\''")\
            }\
        }\
' app/src/main/java/com/example/data/AppDatabase.kt

sed -i '/MIGRATION_15_16/s/MIGRATION_15_16/MIGRATION_15_16, MIGRATION_16_17/' app/src/main/java/com/example/data/AppDatabase.kt
