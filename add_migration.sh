sed -i '/companion object {/a\
\
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {\
            override fun migrate(db: SupportSQLiteDatabase) {\
                db.execSQL("""\
                    INSERT INTO masters (masterType, name, description, displayOrder, isActive, isDeleted, createdDate, modifiedDate)\
                    SELECT type, value, extra, 0, 1, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}\
                    FROM master_data\
                    WHERE NOT EXISTS (\
                        SELECT 1 FROM masters m \
                        WHERE m.masterType = master_data.type AND m.name = master_data.value\
                    )\
                """)\
                db.execSQL("DROP TABLE IF EXISTS master_data")\
            }\
        }\
' app/src/main/java/com/example/data/AppDatabase.kt
