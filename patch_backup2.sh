sed -i '/val mastArr = JSONArray()/d' app/src/main/java/com/example/backup/BackupManager.kt
sed -i '/masterDataList.forEach { master ->/,/root.put("master_data", mastArr)/d' app/src/main/java/com/example/backup/BackupManager.kt
