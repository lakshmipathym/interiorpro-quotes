sed -i '/val masterDataList = repository.allMasterData.first()/d' app/src/main/java/com/example/core/backup/pkg/BackupPackageManager.kt
sed -i '/"master_data" to masterDataList.size,/d' app/src/main/java/com/example/core/backup/pkg/BackupPackageManager.kt
sed -i 's/(summary\["master_data"\] ?: 0) + (summary\["masters_entities"\] ?: 0)/(summary["masters_entities"] ?: 0)/' app/src/main/java/com/example/core/backup/preview/RestorePreviewRepositoryImpl.kt
