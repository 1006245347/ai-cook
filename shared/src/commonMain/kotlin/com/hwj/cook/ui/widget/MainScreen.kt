package com.hwj.cook.ui.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.AgentManager
import com.hwj.cook.global.CODE_IS_DARK
import com.hwj.cook.global.DATA_AGENT_DEF
import com.hwj.cook.global.DATA_AGENT_INDEX
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cHalfGrey80717171
import com.hwj.cook.global.cLightLine
import com.hwj.cook.global.cWhite
import com.hwj.cook.global.getCacheBoolean
import com.hwj.cook.global.onlyDesktop
import com.hwj.cook.global.onlyMobile
import com.hwj.cook.global.printD
import com.hwj.cook.global.removeCacheKey
import com.hwj.cook.global.saveBoolean
import com.hwj.cook.models.AppConfigState
import com.hwj.cook.models.AppIntent
import com.hwj.cook.ui.chat.ChatScreen
import com.hwj.cook.ui.cook.CookScreen
import com.hwj.cook.ui.settings.SettingScreen
import com.hwj.cook.ui.tech.TechScreen
import com.hwj.cook.ui.viewmodel.ChatVm
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.SettingVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.navigation.Navigator
import org.koin.compose.getKoin

val tabList = listOf(
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
    val settingVm = koinViewModel(SettingVm::class)
    val chatVm = koinViewModel(ChatVm::class)
    val sessionId by chatVm.currentSessionState.collectAsState()
    val darkTheme = remember(key1 = CODE_IS_DARK) {
        mutableStateOf(false)
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val pagerState = rememberPagerState(pageCount = { tabList.size }, initialPage = 0)
    var curRoute by remember { mutableStateOf(tabList.first().route) }

    var showNavDialog by remember { mutableStateOf(false) }
    var showAgentDialog by remember { mutableStateOf(false) }
    var barBounds by remember { mutableStateOf<Rect?>(null) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    val agentModelState by chatVm.agentModelState.collectAsState()
    val koin = getKoin()

    val subScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        subScope.launch {
            darkTheme.value = getCacheBoolean(CODE_IS_DARK)
            mainVm.processIntent(AppIntent.ThemeSetIntent)
        }
    }
    LaunchedEffect(initialized) {
        if (!initialized) { //这主页初始化是为了让其他页可以选择模型数据
            settingVm.initialize()
            initialized = true
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
        Surface(color = cAutoBg()) {
            AppScaffold( //脚手架
                navigator, drawerState = drawerState,
                onConversationClicked = { chatId -> //加载历史会话
                    subScope.launch {
                        if (sessionId != chatId) {
                            chatVm.stopReceivingResults()
                        }
                        drawerState.close()
                    }
                },
                onNewConversationClicked = { //抽屉里新增会话、首页顶部也可以新增
                    subScope.launch {
                        chatVm.stopReceivingResults()
                        drawerState.close()
                        chatVm.createSession()
                    }
                },
                onThemeClicked = {
                    subScope.launch {
                        darkTheme.value = !darkTheme.value
                        saveBoolean(CODE_IS_DARK, darkTheme.value)
                        mainVm.processIntent(AppIntent.ThemeSetIntent)
                        if (onlyMobile())
                            drawerState.close()
                    }
                }) { //Tab区域显示 , Drawer+Tab
                Box(Modifier.fillMaxSize()) {
                    if (onlyDesktop()) {
                        Row(Modifier.fillMaxSize()) {
                            if (drawerState.isClosed) {
                                DesktopTabBar(tabList, curRoute) { tab ->
                                    curRoute = tab.route
                                    subScope.launch {
                                        pagerState.scrollToPage(tab.index)
                                    }
                                }
                            }
                            TabNavRoot(
                                navigator,
                                darkTheme.value,
                                drawerState,
                                pagerState,
                                onAgentPop = { rect ->
                                    if (agentModelState != null) {
                                        showAgentDialog = true
                                        if (rect != null) barBounds = rect
                                    }
                                })
                        }
                    } else {
                        Column {
                            Box(Modifier.padding(0.dp).weight(1f)) {
                                TabNavRoot(
                                    navigator, darkTheme.value, drawerState, pagerState,
                                    onAgentPop = { rect ->
                                        if (agentModelState != null) {
                                            showAgentDialog = true
                                            if (rect != null) barBounds = rect
                                        }
                                    },
                                    onShowNav = { rect ->
                                        showNavDialog = true
                                        curRoute = tabList[pagerState.currentPage].route
                                        if (rect != null) barBounds = rect
                                    })
                            }
                            HorizontalDivider(thickness = (0.5f).dp, color = cLightLine())
//                            MobileTabBar(tabList, curRoute) { tab ->
//                                curRoute = tab.route
//                                subScope.launch(Dispatchers.Main) {
//                                    pagerState.scrollToPage(tab.index)
//                                }
//                            }
                        }
                    }
                    if (showNavDialog && barBounds != null) { //手机才显示，desktop不有侧边栏够了
                        PopupBelowAnchor(barBounds!!, onDismiss = { showNavDialog = false }) {
                            PopTabBar(tabList, curRoute) { tab ->
                                curRoute = tab.route
                                showNavDialog = false
                                subScope.launch { pagerState.scrollToPage(tab.index) }
                            }
                        }
                    }

                    if (showAgentDialog && barBounds != null) {
                        PopupTopAnchor(barBounds!!, onDismiss = { showAgentDialog = false }) {
                            PopAgentView { agentName ->//智能体列表  切换
                                subScope.launch {
                                    showAgentDialog = false
                                    chatVm.createAgent(koin, agentName)
                                }
                            }
                        }
                    }
                    ToastHost(  //为了全局显示toast
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .wrapContentWidth()
                            .padding(bottom = 120.dp)
                            .wrapContentHeight()
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

//懒加载  fragment
@Composable
private fun TabNavRoot(
    navigator: Navigator, isDark: Boolean,
    drawerState: DrawerState,
    pagerState: PagerState,
    onAgentPop: (Rect?) -> Unit = {},
    onShowNav: (Rect?) -> Unit = {}
) {
    val subScope = rememberCoroutineScope()
    val chatVm = koinViewModel(ChatVm::class)
    //记录已加载过的页
    val loadedPages = remember { mutableStateListOf<Int>() }
    //页面构造器
    val pages = remember {
        listOf<@Composable () -> Unit>(
            { ChatScreen(navigator) },
            { CookScreen(navigator) },
            { TechScreen(navigator) },
            { SettingScreen(navigator) }
        )
    }
    //为了缓存已打开页面
    val saveableStateHolder = rememberSaveableStateHolder()
    val cachedPages = remember { mutableMapOf<Int, @Composable () -> Unit>() }

    Column(Modifier.fillMaxSize()) {
        if (pagerState.currentPage == 0||pagerState.currentPage==1)
            AppBar(isDark, pagerState, isOpenDrawer = drawerState.isOpen, onClickMenu = {
                subScope.launch {
                    drawerState.open()
                }
            }, onNewChat = {
                subScope.launch {
                    if (pagerState.currentPage != 0)
                        pagerState.scrollToPage(0)
                    //中止、保存、清空当前会话 、新建
                    chatVm.stopReceivingResults()
                    drawerState.close()
                    chatVm.createSession()
                }
            }, onAgentPop = onAgentPop, onShowNav = onShowNav)
        //内容页
        HorizontalPager(userScrollEnabled = false, state = pagerState) { page: Int ->
            if (page !in loadedPages) {
                loadedPages.add(page)
                cachedPages[page] = pages[page]
            }
            saveableStateHolder.SaveableStateProvider(key = "page$page") {
                cachedPages[page]?.invoke()        //页面渲染
            }
        }
    }
}


@Composable
private fun PopTabBar(tabs: List<TabCell>, current: String?, onSelect: (TabCell) -> Unit) {
    Column(Modifier.width(100.dp).wrapContentHeight()) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            Row() {
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    label = {
                        val fontSize by animateFloatAsState(if (selected) 15f else 12f)
                        val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        Text(
                            text = tab.label,
                            fontSize = fontSize.sp,
                            fontWeight = fontWeight,
                            color = if (selected) PrimaryColor else Color.Gray
                        )
                    },
                    icon = {
                        val size by animateDpAsState(if (selected) 24.dp else 20.dp)
                        Icon(
                            imageVector = if (tab.index == 0) Icons.Default.Pending else if (tab.index == 1) Icons.Default.Book else if (tab.index == 2) Icons.Default.Memory else Icons.Default.Settings,
                            contentDescription = tab.label,
                            modifier = Modifier.size(size),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        selectedIconColor = cWhite(),
                        selectedTextColor = cAutoTxt(true),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    }
}

//智能体列表视图
@Composable
private fun PopAgentView(onClicked: (String) -> Unit) {//  onClick: () -> Unit
    val list = AgentManager.validAgentList()
    Column(Modifier.padding(top = 10.dp).width(120.dp).wrapContentHeight()) {
        list.forEach { cell ->
            Column(
                Modifier.align(Alignment.CenterHorizontally).height(35.dp)
                    .clickable(onClick = { onClicked(cell.name) })
            ) {
                Text(
                    "Agent ${cell.name}",
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                HorizontalDivider(thickness = 1.dp, color = cHalfGrey80717171())
            }
        }
    }
}

// 桌面端左侧Tab栏  ,drawer展开时，这里要隐藏
@Composable
private fun DesktopTabBar(tabs: List<TabCell>, current: String?, onSelect: (TabCell) -> Unit) {
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    Column(
        Modifier.fillMaxHeight().width(60.dp).background(cAutoBg()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(50.dp))
        tabs.forEach { tab ->
            val selected = current == tab.route
            val fontSize by animateFloatAsState(if (selected) 15f else 12f)
            val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            val size by animateDpAsState(if (selected) 24.dp else 20.dp)
            Column(
                Modifier.padding(10.dp)
                    .fillMaxWidth()
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab.label,
                    fontSize = fontSize.sp,
                    fontWeight = fontWeight,
                    color = if (selected) PrimaryColor else Color.Gray
                )
                Icon(
                    imageVector = if (tab.index == 0) Icons.Default.Pending else if (tab.index == 1) Icons.Default.Book else if (tab.index == 2) Icons.Default.Memory else Icons.Default.Settings,
                    contentDescription = tab.label,
                    modifier = Modifier.size(size),
                    tint = if (selected) PrimaryColor else Color.Gray
                )

            }
        }
    }
}

@Composable
private fun MainInit(state: AppConfigState) {
    Surface(modifier = Modifier.fillMaxSize(), color = cAutoBg()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (state.isLoading) {
                Text("加载中...", color = MaterialTheme.colorScheme.secondary)
            } else if (state.error != null) {
                Text(state.error)
            }
        }
    }
}

// 移动端底部Tab栏 ,选中样式   bar上再放编辑框很怪异，看其他是编辑框居中点击跳新页面，不太想要这
@Composable
private fun MobileTabBar(tabs: List<TabCell>, current: String?, onSelect: (TabCell) -> Unit) {
    NavigationBar(tonalElevation = 0.dp, containerColor = cAutoBg()) {
        tabs.forEach { tab ->
            val selected = current == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                label = {
                    val fontSize by animateFloatAsState(if (selected) 15f else 12f)
                    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    Text(
                        text = tab.label,
                        fontSize = fontSize.sp,
                        fontWeight = fontWeight,
                        color = if (selected) cWhite() else Color.Gray
                    )
                },
                icon = {
                    val size by animateDpAsState(if (selected) 24.dp else 20.dp)
                    Icon(
                        imageVector = if (tab.index == 0) Icons.Default.Pending else if (tab.index == 1) Icons.Default.Book else if (tab.index == 2) Icons.Default.Memory else Icons.Default.Settings,
                        contentDescription = tab.label,
                        modifier = Modifier.size(size),
                        tint = if (selected) cWhite() else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = cWhite(),
                    selectedTextColor = cAutoTxt(true),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
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