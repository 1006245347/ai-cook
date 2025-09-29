package com.hwj.cook.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwj.cook.global.onlyMobile
import com.hwj.cook.ui.viewmodel.MainVm
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

//期望jvm端是边栏，phone是抽屉 ，tab不需要滑动切换
@Composable
fun AppScaffold(
    navigator: Navigator,
    drawerState: DrawerState = rememberDrawerState(initialValue = Closed),
    onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    onThemeClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    val mainVm = koinViewModel(MainVm::class)
    LaunchedEffect(true) {
        mainVm.initialize()
    }

    if (onlyMobile()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContentColor = MaterialTheme.colorScheme.background) {
                    Box {
                        AppDrawer(
                            navigator,
                            onConversationClicked,
                            onNewConversationClicked,
                            onThemeClicked
                        )
                    }
                }
            }, content = content //内容布局
        )
    } else { //桌面端抽屉和内容并存 ,默认展开抽屉
        PermanentNavigationDrawer(drawerContent = {
            PermanentDrawerSheet(
                drawerContentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.width(if (drawerState.isClosed) 0.dp else 240.dp)
            ) {
                Box {
                    AppDrawer(
                        navigator,
                        onConversationClicked,
                        onNewConversationClicked,
                        onThemeClicked
                    )
                }
            }
        }) { content() }
    }
}