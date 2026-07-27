sed -i '/val clientCount = /d' app/src/main/java/com/example/core/backup/WorkspaceManagerImpl.kt
sed -i '/clientCount = clientCount,/d' app/src/main/java/com/example/core/backup/WorkspaceManagerImpl.kt
sed -i '/val clientCount: Int = 0,/d' app/src/main/java/com/example/core/backup/WorkspaceManager.kt
