package com.hwj.cook.except

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Looper
import android.util.Patterns
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.LoadingThinking
import com.hwj.cook.global.thinkingTip
import com.hwj.cook.ui.widget.BotCommonCard
import com.hwj.cook.ui.widget.BotCommonMenu
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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

@Composable
actual fun BotMessageCard(msg: String) {
    if ( msg == thinkingTip) {
        LoadingThinking(msg)
    } else {
        BotCommonCard(msg)
    }
}


@Composable
actual fun BotMsgMenu(message: String){
    BotCommonMenu(message)
}

actual class ClipboardHelper(private val context: Context) {

    actual fun copyToClipboard(text: String) {
        val chunkSize = 2 * 1020 * 1024 //2mb
        val safeTxt = if (text.length > chunkSize) {
            text.substring(0, chunkSize)
        } else text
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copy", safeTxt)
        clipboard.setPrimaryClip(clip)
    }

    actual fun readFromClipboard(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        return clip?.getItemAt(0)?.text.toString()
    }
}


