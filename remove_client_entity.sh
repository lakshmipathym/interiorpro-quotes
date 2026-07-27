sed -i '/@Entity(/,/)\])/d' app/src/main/java/com/example/data/Entities.kt
sed -i '/data class Client(/,/val modifiedDate: Long = System.currentTimeMillis()\n)/d' app/src/main/java/com/example/data/Entities.kt
