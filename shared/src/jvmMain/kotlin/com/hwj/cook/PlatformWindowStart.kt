package com.hwj.cook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import com.hwj.cook.capture.LocalMainWindow
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.ui.widget.PlatformAppStart
import io.github.vinceglb.filekit.FileKit
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
fun PlatformWindowStart(
    windowState: WindowState, isShowWindowState: MutableState<Boolean>,
    onWindowChange: @Composable (ComposeWindow, Boolean) -> Unit,
    onCloseRequest: () -> Unit
) {
    var isShow by isShowWindowState
    return Window(
        onCloseRequest = {
            isShowWindowState.value = false
//            onCloseRequest() //放到托盘，不关闭
        },
        title = "", state = windowState, undecorated = false //标题布局
    ) {
        val window = this.window
        window.minimumSize = Dimension(650, 450)
        //想动态更改标题布局颜色，windows无效
        window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        window.background = java.awt.Color(32, 32, 32)
        val titleBarPadding = when {
            System.getProperty("os.name").contains("Mac") -> 33.dp
            System.getProperty("os.name").contains("Windows") -> 0.dp
            else -> 0.dp
        }

        ProvidePreComposeLocals {
            CompositionLocalProvider(
                LocalMainWindow provides window,
            ) {
                onWindowChange(LocalMainWindow.current, isShow)
                ThemeChatLite {
                    Surface(Modifier.fillMaxSize().padding(top = titleBarPadding)) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PlatformAppStart()
                        }
                    }
                }
            }
        }
    }
}