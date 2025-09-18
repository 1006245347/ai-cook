package com.hwj.cook.global

import androidx.compose.runtime.Composable
import com.hwj.cook.ui.widget.FirstScreen
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun NavigateRoute(navigator: Navigator) {
    NavHost(navigator = navigator, initialRoute = NavigationScene.First.path) {
        scene(NavigationScene.First.path) {
            FirstScreen(navigator)
        }
        scene(NavigationScene.Welcome.path) { }
    }
}

sealed class NavigationScene(val path: String, val title: String? = null) {
    object First : NavigationScene("/app", "first")
    object Welcome : NavigationScene("/app/welcome", "welcome")
    object Main : NavigationScene("/main", "main")
}