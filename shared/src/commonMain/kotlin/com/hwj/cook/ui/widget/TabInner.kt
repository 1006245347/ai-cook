package com.hwj.cook.ui.widget

import androidx.compose.runtime.Composable
import com.hwj.cook.global.printLog
import com.hwj.cook.ui.chat.ChatScreen
import com.hwj.cook.ui.cook.CookScreen
import com.hwj.cook.ui.settings.SettingScreen
import com.hwj.cook.ui.tech.TechScreen
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun TabInSide(tab: TabCell, content: @Composable () -> Unit) {
    content()
}

@Composable
fun SubOfTab(index: Int, globalNavigator: Navigator) {
    if (index == 0) {
        ChatScreen(globalNavigator)
    } else if (index == 1) {
        CookScreen(globalNavigator)
    } else if (index == 2) {
        TechScreen(globalNavigator)
    } else {
        SettingScreen(globalNavigator)
    }
}

