package com.hwj.cook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import com.hwj.cook.capture.LocalMainWindow
import moe.tlaster.precompose.ProvidePreComposeLocals
import java.awt.Dimension

//编译运行命令 ./gradlew :desktop:run
//打包命令 ./gradlew packageDistributionForCurrentOS
//安装包路径 build/compose/binaries/main/
//build/compose/binaries/main/exe/
//build/compose/binaries/main/deb/
//Ubuntu/Debian: MyApp-1.0.0.deb

//control +  option +O       control + C 中断调试
//Tray 系统托盘
@Composable
fun  PlatformWindowStart (    windowState: WindowState, isShowWindowState: MutableState<Boolean>,
                               onWindowChange: @Composable (ComposeWindow, Boolean) -> Unit,
                               onCloseRequest: () -> Unit){
    var isShow by isShowWindowState
    return Window(
        onCloseRequest = {
            isShowWindowState.value = false
//            onCloseRequest() //放到托盘，不关闭
        },
        title = "hwj-ai-cook", state = windowState,
    ) {
        val window = this.window
        window.minimumSize = Dimension(650, 450)

        ProvidePreComposeLocals {
            CompositionLocalProvider(
                LocalMainWindow provides window,
            ) {
                onWindowChange(LocalMainWindow.current, isShow)
//                ThemeChatLite {
                    Surface(Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
//                            PlatformAppStart()
                        }
                    }
//                }
            }
        }
    }
}