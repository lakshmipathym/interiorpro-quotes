perl -0777 -pi -e 's/        val masterDataList = repository\.allMasterData\.first\(\)\n//' app/src/main/java/com/example/backup/BackupManager.kt

perl -0777 -pi -e 's/        val mastArr = JSONArray\(\)\n        masterDataList\.forEach \{ md ->\n            val mdObj = JSONObject\(\)\n            mdObj\.put\("id", md\.id\)\n            mdObj\.put\("type", md\.type\)\n            mdObj\.put\("value", md\.value\)\n            mdObj\.put\("extra", md\.extra\)\n            mastArr\.put\(mdObj\)\n        \}\n        root\.put\("master_data", mastArr\)\n//s' app/src/main/java/com/example/backup/BackupManager.kt

perl -0777 -pi -e 's/                if \(root\.has\("master_data"\)\) \{.*?                    db\.masterDataDao\(\)\.insertAll\(masters\)\n                \}\n//s' app/src/main/java/com/example/backup/BackupManager.kt
