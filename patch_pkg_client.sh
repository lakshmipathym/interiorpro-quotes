sed -i '/val clients = db\.clientDao().getAllClients().first()/d' app/src/main/java/com/example/core/backup/pkg/BackupPackageManager.kt
sed -i '/"clients" to clients.size,/d' app/src/main/java/com/example/core/backup/pkg/BackupPackageManager.kt
