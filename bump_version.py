import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

content = content.replace("version = 15,", "version = 16,")

migration_15_16 = """
        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawWidth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawHeight TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawDepth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedHeight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedDepth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN billableQuantity REAL NOT NULL DEFAULT 0.0")
            }
        }
"""

content = content.replace(
    ".addMigrations(MIGRATION_10_11, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)", 
    ".addMigrations(MIGRATION_10_11, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)"
)

# Insert the migration before INSTANCE
content = content.replace(
    "        @Volatile",
    migration_15_16 + "\n        @Volatile"
)

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)

