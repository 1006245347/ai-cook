package com.hwj.cook.global

import androidx.compose.runtime.Composable
import com.hwj.cook.ui.cook.BookReadScreen
import com.hwj.cook.ui.settings.SettingDefScreen
import com.hwj.cook.ui.settings.SettingEditModelScreen
import com.hwj.cook.ui.widget.FirstScreen
import com.hwj.cook.ui.widget.MainScreen
import com.hwj.cook.ui.widget.WelcomeScreen
import io.ktor.http.decodeURLQueryComponent
import io.ktor.util.decodeBase64String
import moe.tlaster.precompose.navigation.NavHost
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.path

@Composable
fun NavigateRoute(navigator: Navigator) {

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
        scene(NavigationScene.BookRead.path + "/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.path<String>("bookId")
            bookId?.let {
                BookReadScreen(navigator, bookId)
            }
        }
        scene(NavigationScene.SettingsEdit.path + "/{index}") { backStackEntry ->
            val modelId = backStackEntry.path<Int>("index")
            modelId?.let {
                SettingEditModelScreen(navigator,modelId)
            }
        }
        scene(NavigationScene.SettingsDef.path){
            SettingDefScreen(navigator)
        }
    }
}


sealed class NavigationScene(val path: String, val title: String? = null) {
    object First : NavigationScene("/app", "first")
    object Welcome : NavigationScene("/app/welcome", "welcome")
    object Main : NavigationScene("/main", "main")
    object BookRead : NavigationScene("/main/cook/read")
    object SettingsEdit : NavigationScene("/main/settings/edit")
    object SettingsDef: NavigationScene("/main/settings/def")
}