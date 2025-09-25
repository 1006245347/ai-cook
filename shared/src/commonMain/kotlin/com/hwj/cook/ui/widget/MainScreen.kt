package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.hwj.cook.global.CODE_IS_DARK
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.global.cDeepLine
import com.hwj.cook.global.cLowOrange
import com.hwj.cook.global.cWhite
import com.hwj.cook.global.getCacheBoolean
import com.hwj.cook.global.onlyDesktop
import com.hwj.cook.global.printLog
import com.hwj.cook.global.saveBoolean
import com.hwj.cook.models.AppConfigState
import com.hwj.cook.models.UiIntent
import com.hwj.cook.ui.chat.ChatScreen
import com.hwj.cook.ui.viewmodel.MainVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.rememberNavigator

val tabList = listOf<TabCell>(
    TabCell("/main/chat", "AI", 0),
    TabCell("/main/cook", "菜谱", 1),
    TabCell("/main/tech", "知识", 2),
    TabCell("/main/settings", "设置", 3)
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
    val curTab = navigator.currentEntry.collectAsState(null).value
    val curRoute = curTab?.route?.route
    val pagerState = rememberPagerState(pageCount = { tabList.size }, initialPage = 0)

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
//    CheckNetConnected()
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
                }) { //Tab区域显示
                Box(Modifier.fillMaxSize()) {
                    HorizontalDivider(thickness = (0.5f).dp, color = cDeepLine())

                    if (onlyDesktop()) {
                        Row(Modifier.fillMaxSize()) {
                            DesktopTabBar(tabList, curRoute) { tab ->
                                subScope.launch {
                                    pagerState.animateScrollToPage(tab.index)
                                }
                            }

                            TabNavRoot(navigator, drawerState, pagerState)
                        }
                    } else {
                        Scaffold(bottomBar = {
                            MobileTabBar(tabList, curRoute) { tab ->
                                subScope.launch(Dispatchers.Main) {
                                    pagerState.scrollToPage(tab.index)
                                }
                            }
                        }) { padding ->
                            Box(Modifier.padding(padding).fillMaxSize()) {
                                TabNavRoot(navigator, drawerState, pagerState)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabNavRoot(navigator: Navigator, drawerState: DrawerState, pagerState: PagerState) {
    val subScope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(cWhite())) {
        Column(Modifier.fillMaxSize()) {
            AppBar(onClickMenu = {
                subScope.launch {
                    drawerState.open()
                }
            }, onNewChat = {

            })

            HorizontalPager(userScrollEnabled = false, state = pagerState) { page: Int ->
                TabInSide(tabList[page], { SubOfTab(page, navigator, navigator) })
            }
        }
    }
}

@Composable
private fun TabNavRoot2(navigator: Navigator) {
//两层嵌套的NavHost,注意全局的和内部的navigator
    val stateHolder = rememberSaveableStateHolder()
//    val insideNavigator = rememberNavigator()
    val insideNavigator = navigator
//    NavHost(insideNavigator, initialRoute = tabList.first().route) {
//        tabList.forEachIndexed { index, tab ->
//            scene(tab.route) { s ->
//                //缓存页面状态
//                stateHolder.SaveableStateProvider(tab.route) {
//                    TabInSide(tab, { SubOfTab(index, navigator, insideNavigator) })
//                }
//            }
//        }
//    }

}

// 移动端底部Tab栏
@Composable
private fun MobileTabBar(tabs: List<TabCell>, current: String?, onSelect: (TabCell) -> Unit) {
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { onSelect(tab) },
                label = { Text(tab.label) },
                icon = {}
            )
        }
    }
}

// 桌面端左侧Tab栏
@Composable
private fun DesktopTabBar(tabs: List<TabCell>, current: String?, onSelect: (TabCell) -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(100.dp).background(cLowOrange()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tabs.forEach { tab ->
            Box(
                Modifier.fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onSelect(tab) }
                    .background(if (current == tab.route) cLowOrange() else Color.Transparent)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tab.label)
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