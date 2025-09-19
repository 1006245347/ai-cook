package com.hwj.cook.global

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.hwj.cook.ui.widget.FirstScreen
import com.hwj.cook.ui.widget.MainScreen
import com.hwj.cook.ui.widget.TabInSide
import com.hwj.cook.ui.widget.WelcomeScreen
import com.hwj.cook.ui.widget.checkTab
import com.hwj.cook.ui.widget.tabList
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun NavigateRoute(navigator: Navigator) {

    val stateHolder = rememberSaveableStateHolder()

    NavHost(navigator = navigator, initialRoute = NavigationScene.First.path) {
        scene(NavigationScene.First.path) {
            FirstScreen(navigator)
        }
        scene(NavigationScene.Welcome.path) {
            WelcomeScreen(navigator)
        }
        scene(NavigationScene.Main.path) {
            MainScreen(navigator)
        }

        //主页tab
        tabList.forEachIndexed { index, tab ->
            scene(tab.route) { s ->
                //缓存页面状态
                stateHolder.SaveableStateProvider(tab.route) {
                    TabInSide(tab, { checkTab(index, navigator) })
                }
            }
        }


    }
}


sealed class NavigationScene(val path: String, val title: String? = null) {
    object First : NavigationScene("/app", "first")
    object Welcome : NavigationScene("/app/welcome", "welcome")
    object Main : NavigationScene("/main", "main")
}