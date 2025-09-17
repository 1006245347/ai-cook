package com.hwj.cook.except

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun ToolTipCase(modifier: Modifier?, tip: String, content: @Composable () -> Unit) {
    content()
}

actual fun isMainThread(): Boolean {
    return NSThread.isMainThread
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