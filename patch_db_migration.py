import re

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'r') as f:
    content = f.read()

pattern = r"""db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN rawWidth TEXT NOT NULL DEFAULT ''"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN rawHeight TEXT NOT NULL DEFAULT ''"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN rawDepth TEXT NOT NULL DEFAULT ''"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN parsedWidth REAL NOT NULL DEFAULT 0\.0"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN parsedHeight REAL NOT NULL DEFAULT 0\.0"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN parsedDepth REAL NOT NULL DEFAULT 0\.0"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN rawQuantity REAL NOT NULL DEFAULT 0\.0"\)
                db\.execSQL\("ALTER TABLE quotation_item ADD COLUMN billableQuantity REAL NOT NULL DEFAULT 0\.0"\)
                
                db\.execSQL\("ALTER TABLE quotation ADD COLUMN taxableAmount REAL NOT NULL DEFAULT 0\.0"\)"""

replacement = """db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawWidth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawHeight TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawDepth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedHeight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN parsedDepth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN rawQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_template ADD COLUMN billableQuantity REAL NOT NULL DEFAULT 0.0")
                
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawWidth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawHeight TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawDepth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN parsedWidth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN parsedHeight REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN parsedDepth REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN rawQuantity REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE quotation_item ADD COLUMN billableQuantity REAL NOT NULL DEFAULT 0.0")
                
                db.execSQL("ALTER TABLE quotation ADD COLUMN taxableAmount REAL NOT NULL DEFAULT 0.0")"""

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/data/AppDatabase.kt', 'w') as f:
    f.write(content)
