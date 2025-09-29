package com.hwj.cook.except

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.Network.*
import platform.darwin.*

@Composable
actual fun ToolTipCase(modifier: Modifier?, tip: String, content: @Composable () -> Unit) {
    content()
}

actual fun isMainThread(): Boolean {
    return NSThread.isMainThread
}

class IOSNetworkObserver : NetworkObserver {

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create(
        label = "org.yangdai.kori.networkMonitor",
        attr = DISPATCH_QUEUE_SERIAL_WITH_AUTORELEASE_POOL
    )
    override fun observe(): Flow<NetworkObserver.Status> {
        return callbackFlow {
            nw_path_monitor_set_update_handler(monitor) { path ->
                val status = nw_path_get_status(path)
                when (status) {
                    nw_path_status_satisfied -> launch { send(NetworkObserver.Status.Connected) }
                    else -> launch { send(NetworkObserver.Status.Disconnected) }
                }
            }

            nw_path_monitor_set_queue(monitor, queue)
            nw_path_monitor_start(monitor)

            awaitClose {
                nw_path_monitor_cancel(monitor)
            }
        }.distinctUntilChanged()
    }
}

@Composable
actual fun ScreenShotPlatform(onSave: (String?) -> Unit) {
}

@Composable
actual fun BringMainWindowFront(){
}

actual fun getShotCacheDir(): String? {
    return null
}

//需在 Info.plist 添加白名单（iOS 9+ 要求）：
//LSApplicationQueriesSchemes
//http
//https
@Composable
actual fun switchUrlByBrowser(url: String) {
    val nsUrl = NSURL(string = url)
    UIApplication.sharedApplication.openURL(nsUrl)
}