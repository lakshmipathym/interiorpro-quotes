import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

pattern = r'db\.execSQL\("ALTER TABLE quotation ADD COLUMN taxableAmount REAL NOT NULL DEFAULT 0.0"\)'
replacement = """db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawWidth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawHeight TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawDepth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedHeight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedDepth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN billableQuantity REAL NOT NULL DEFAULT 0.0")
                
                db.execSQL("ALTER TABLE quotation ADD COLUMN taxableAmount REAL NOT NULL DEFAULT 0.0")"""

content = content.replace('db.execSQL("ALTER TABLE quotation ADD COLUMN taxableAmount REAL NOT NULL DEFAULT 0.0")', replacement)

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)
