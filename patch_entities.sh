perl -0777 -pi -e 's/\@Entity\(\s*tableName = "client"\s*\)\s*data class Client\(.*?\)\n//s' app/src/main/java/com/example/data/Entities.kt
