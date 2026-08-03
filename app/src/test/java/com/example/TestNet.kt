package com.example
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities

@RunWith(RobolectricTestRunner::class)
class TestNet {
    @Test
    fun testNet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val shadowCm = shadowOf(cm)
        
        shadowCm.setDefaultNetworkActive(true)
        val activeNetwork = cm.activeNetwork
        
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        
        shadowCm.setNetworkCapabilities(activeNetwork, networkCapabilities)
        
        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        if (capabilities != null) {
            println("Capabilities: " + capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        } else {
            println("Capabilities is null")
        }
    }
}
