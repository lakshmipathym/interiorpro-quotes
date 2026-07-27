sed -i '/import com\.example\.data\.ClientRepository/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/import com\.example\.ui\.client\.ClientViewModel/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/import com\.example\.ui\.client\.ClientViewModelFactory/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/import com\.example\.ui\.client\.ClientsScreen/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/import com\.example\.ui\.client\.AddEditClientScreen/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/private val clientRepository by lazy { ClientRepository(database) }/d' app/src/main/java/com/example/MainActivity.kt
