sed -i '/val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false/d' app/src/main/java/com/example/core/sync/SyncManagerImpl.kt
sed -i '/val network = connectivityManager.activeNetwork ?: return false/d' app/src/main/java/com/example/core/sync/SyncManagerImpl.kt
sed -i '/val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false/d' app/src/main/java/com/example/core/sync/SyncManagerImpl.kt
sed -i '/return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)/d' app/src/main/java/com/example/core/sync/SyncManagerImpl.kt
