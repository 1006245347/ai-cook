package com.hwj.cook.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.hwj.cook.global.CODE_IS_DARK
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.global.getCacheBoolean
import com.hwj.cook.global.saveBoolean
import com.hwj.cook.models.AppConfigState
import com.hwj.cook.models.UiIntent
import com.hwj.cook.ui.viewmodel.MainVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.navigation.Navigator

val tabList = listOf<TabCell>(
    TabCell("chat", "AI问答", 0),
    TabCell("cook", "菜谱", 1),
    TabCell("tech", "知识", 2),
    TabCell("settings", "设置", 3)
)

data class TabCell(val route: String, val label: String, val index: Int)


//在 Android Compose 里，所谓 副作用 (Side Effects)，指的是那些不直接属于 UI 声明式绘制范畴，但需要在 生命周期或状态变化时触发的一些逻辑。常见场景比如：
//
//启动一个一次性的任务（比如网络请求、动画启动）。
//
//订阅和取消订阅外部回调（如传感器、广播接收器）。
//
//同步 Compose 内部状态和外部系统状态（如更新系统 UI、Log 打印）。
//compose多层嵌套时，每层的副作用只管理自己的生命周期，不会相互覆盖或失效。
@Composable
fun MainScreen(navigator: Navigator) {
    val mainVm = koinViewModel(MainVm::class)
    val darkTheme = remember(key1 = CODE_IS_DARK) {
        mutableStateOf(false)
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerOpen by mainVm.drawerShouldBeOpened.collectAsState()
    //默认跳转第一个tab
    val subScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        subScope.launch {
            darkTheme.value = getCacheBoolean(CODE_IS_DARK)
            mainVm.processIntent(UiIntent.ThemeSetIntent)
        }
    }
    val focusManager = LocalFocusManager.current
    BackHandler {
        if (drawerState.isOpen) {
            subScope.launch {
                drawerState.close()
            }
        } else {
            focusManager.clearFocus()
        }
    }
    CheckNetConnected()
    ThemeChatLite(isDark = darkTheme.value) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppScaffold( //脚手架
                navigator, drawerState = drawerState,
                onConversationClicked = { chatId -> },
                onNewConversationClicked = {},
                onThemeClicked = {
                    subScope.launch {
                        darkTheme.value = !darkTheme.value
                        saveBoolean(CODE_IS_DARK, darkTheme.value)
                        mainVm.processIntent(UiIntent.ThemeSetIntent)
                        drawerState.close()
                    }
                }) { //Tab的内容
                Box(Modifier.fillMaxSize()) {

                }
            }
        }
    }
}

@Composable
fun MainInit(state: AppConfigState) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) {
                Text("加载中...", color = MaterialTheme.colorScheme.secondary)
            } else if (state.error != null) {
                Text(state.error)
            }
        }
    }
}

@Composable
fun CheckNetConnected() {
    val subScope = rememberCoroutineScope()
    val mainVm = koinViewModel(MainVm::class)
    val isConnected = mainVm.isNetState.collectAsState()
    LaunchedEffect(Unit) {
        subScope.launch(Dispatchers.IO) {
            mainVm.checkNetStatus()
        }
    }
    if (!isConnected.value) ToastUtils.show("net err!")
}