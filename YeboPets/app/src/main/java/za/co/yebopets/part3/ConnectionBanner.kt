package za.co.yebopets.part3
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
@Composable fun ConnectionBanner() {
    val manager=LocalContext.current.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    fun connected()=manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
    var online by remember {mutableStateOf(connected())}
    DisposableEffect(manager) {
        val callback=object:ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network:Network,capabilities:NetworkCapabilities) {online=connected()}
            override fun onLost(network:Network) {online=connected()}
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose {manager.unregisterNetworkCallback(callback)}
    }
    if(!online) Text(T("Offline: showing cached pets. Availability and distance may have changed."))
}