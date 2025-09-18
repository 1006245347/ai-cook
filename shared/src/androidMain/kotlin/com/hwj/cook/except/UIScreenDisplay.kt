package com.hwj.cook.except

import android.content.Intent
import android.os.Looper
import android.util.Patterns
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ToolTipCase(modifier: Modifier?, tip: String, content: @Composable () -> Unit) {
    content()
}

actual fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread == Thread.currentThread()
//        .also { printD("thread=${it.name}") }
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

@Composable
actual fun switchUrlByBrowser(url: String) {
    if (Patterns.WEB_URL.matcher(url).matches()) {
        LocalContext.current.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}

fun c(){
    object {}.javaClass.getResource("").path
}