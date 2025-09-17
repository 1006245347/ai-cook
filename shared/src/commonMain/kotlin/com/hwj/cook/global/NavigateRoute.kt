package com.hwj.cook.global

import androidx.compose.runtime.Composable
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun NavigateRoute(navigator: Navigator) {
    NavHost(navigator = navigator, initialRoute = NavigationScene.App.path) {
        scene(NavigationScene.App.path) {
//            App(navigator) {}
        }
        scene(NavigationScene.Welcome.path) { }
    }
}

sealed class NavigationScene(val path: String, val title: String? = null) {
    object App : NavigationScene("/app", "app")
    object Welcome : NavigationScene("/app/welcome", "welcome")
    object Main : NavigationScene("/main", "main")
}