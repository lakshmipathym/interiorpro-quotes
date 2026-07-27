#!/bin/bash
sed -i 's/MasterData::class,//g' app/src/main/java/com/example/data/AppDatabase.kt
sed -i 's/version = 10,/version = 11,/g' app/src/main/java/com/example/data/AppDatabase.kt
sed -i '/abstract fun masterDataDao(): MasterDataDao/d' app/src/main/java/com/example/data/AppDatabase.kt
sed -i '/INSERT INTO master_data/d' app/src/main/java/com/example/data/AppDatabase.kt
