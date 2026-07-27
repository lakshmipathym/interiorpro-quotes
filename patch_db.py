import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

# Replace version = 14 with version = 15
content = content.replace('version = 14,', 'version = 15,')

migration_14_15 = """
        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawWidth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawHeight TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawDepth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN parsedWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN parsedHeight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN parsedDepth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN billableQuantity REAL NOT NULL DEFAULT 0.0")
                
                db.execSQL("ALTER TABLE quotation ADD COLUMN taxableAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation ADD COLUMN amountInWords TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyNameSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyOwnerNameSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyPhoneSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyEmailSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyAddressSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyGstinSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyBankNameSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyAccountNameSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyAccountNumberSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyIfscSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyBranchSnapshot TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation ADD COLUMN companyUpiIdSnapshot TEXT NOT NULL DEFAULT ''")
            }
        }
"""

content = content.replace('        @Volatile', migration_14_15 + '\n        @Volatile')

# Add to addMigrations
content = content.replace('addMigrations(MIGRATION_10_11, MIGRATION_12_13, MIGRATION_13_14)', 'addMigrations(MIGRATION_10_11, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)')

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)
