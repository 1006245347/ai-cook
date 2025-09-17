package com.hwj.cook.except

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

//处理desktop端鼠标指向图标文字提示
@Composable
expect fun ToolTipCase(modifier: Modifier?=null,tip: String, content: @Composable () -> Unit)


//缓存截图的目录
expect fun getShotCacheDir():String?

//链接跳转浏览器
@Composable
expect fun switchUrlByBrowser(url:String)

@Composable
expect fun BringMainWindowFront()

//是否在主线程
expect fun isMainThread():Boolean

//desktop截图
@Composable
expect fun ScreenShotPlatform(onSave: (String?) -> Unit)