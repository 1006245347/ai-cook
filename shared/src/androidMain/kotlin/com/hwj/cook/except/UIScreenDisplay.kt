package com.hwj.cook.except

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Looper
import android.util.Patterns
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.net.URLEncoder

@Composable
actual fun ToolTipCase(modifier: Modifier?, tip: String, content: @Composable () -> Unit) {
    content()
}

actual fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread == Thread.currentThread()
//        .also { printD("thread=${it.name}") }
}

class AndroidNetworkObserver(context: Context) : NetworkObserver {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    override fun observe(): Flow<NetworkObserver.Status> {
        return callbackFlow @androidx.annotation.RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(NetworkObserver.Status.Connected) }
                }

                override fun onLost(network: Network) {
                    launch { send(NetworkObserver.Status.Disconnected) }
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }
}

@Composable
actual fun ScreenShotPlatform(onSave: (String?) -> Unit) {
}

@Composable
actual fun BringMainWindowFront() {

}

actual fun getShotCacheDir(): String? {
    return null
}

@Composable
actual fun switchUrlByBrowser(url: String) {
    if (Patterns.WEB_URL.matcher(url).matches()) {
        LocalContext.current.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}

