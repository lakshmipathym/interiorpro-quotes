sed -i '/val context = ApplicationProvider.getApplicationContext<Context>()/a \
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager\
        org.robolectric.Shadows.shadowOf(cm).setActiveNetworkInfo(org.robolectric.shadows.ShadowNetworkInfo.newInstance(android.net.NetworkInfo.DetailedState.CONNECTED, android.net.ConnectivityManager.TYPE_WIFI, 0, true, android.net.NetworkInfo.State.CONNECTED))\
' app/src/test/java/com/example/GoogleDriveSyncTest.kt
