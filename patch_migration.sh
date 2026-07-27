sed -i 's/.fallbackToDestructiveMigration(true)/.addMigrations(MIGRATION_10_11)\n                .fallbackToDestructiveMigration(true)/' app/src/main/java/com/example/data/AppDatabase.kt
