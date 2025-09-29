package com.hwj.cook.except

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.capture.LocalMainWindow
import com.hwj.cook.capture.getPlatformCacheImgDir11
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.Frame
import java.net.URI

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun ToolTipCase(modifier: Modifier?, tip: String, content: @Composable () -> Unit) {
    TooltipArea(
        tooltip = { //鼠标移动浮动指向提示
            Surface(modifier = Modifier.padding(2.dp)) {
                Text(text = tip, Modifier.padding(4.dp), fontSize = 12.sp)
            }
        },
        delayMillis = 100,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(5.dp, 5.dp)),
        modifier = modifier ?: Modifier
    ) {
        content()
    }
}

actual fun isMainThread(): Boolean {
    return Thread.currentThread().name == "main"
}

class JvmNetworkObserver: NetworkObserver{
    override fun observe(): Flow<NetworkObserver.Status> {
        return flowOf(NetworkObserver.Status.Connected).distinctUntilChanged()
    }
}

@Composable
actual fun ScreenShotPlatform(onSave: (String?) -> Unit) {
//    val mainWindow = LocalMainWindow.current
//    val chatViewModel = koinViewModel(ChatViewModel::class)
//    val subScope = rememberCoroutineScope()
//    val isShotState = chatViewModel.isShotState.collectAsState().value
//    val isHotShotState = chatViewModel.isShotByHotKeyState.collectAsState().value
//    if (isShotState && onlyDesktop()) {
//        ScreenshotOverlay11(mainWindow = mainWindow, onCapture = { pic ->
//            val file = saveToFile11(pic)
//            onSave(file)
//            if (isHotShotState && file != null) {
//                EventHelper.post(Event.AnalyzePicEvent(file))
//                GlobalMouseHook9.bring2Front()
//            }
//            //新建会话会清除
//        }, onCancel = {
//            subScope.launch(Dispatchers.Main) {
//                chatViewModel.shotScreen(false)
//            }
//        })
//    }
}

@Composable
actual fun BringMainWindowFront() {
    showMainWindow(true)
}

@Composable
fun showMainWindow(flag: Boolean) {
    val subScope = rememberCoroutineScope()
    println("showMain>$flag") //托盘时没法响应 isShowWindowState
    if (flag) {
        LocalMainWindow.current.apply {
            subScope.launch(Dispatchers.Main) {
                isMinimized = false
                isVisible = true
                toFront()
                state = Frame.NORMAL
            }
        }
    } else {
        LocalMainWindow.current.isVisible = false
    }
}

actual fun getShotCacheDir(): String? {
    return getPlatformCacheImgDir11().absolutePath
}

@Composable
actual fun switchUrlByBrowser(url: String) {
    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(url))
    }
}