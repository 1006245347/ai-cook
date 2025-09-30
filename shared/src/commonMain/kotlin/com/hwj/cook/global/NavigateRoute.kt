package com.hwj.cook.global

import androidx.compose.runtime.Composable
import com.hwj.cook.ui.cook.BookReadScreen
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
//            BookReadScreen(navigator, clicked = { node->  navigator.navigate(NavigationScene.BookRead.path)})

            val bookId = backStackEntry.path<String>("bookId")
            printLog("bookId>$bookId")
            printLog("decode>${bookId?.decodeURLQueryComponent()}")
            bookId?.let {
                BookReadScreen(navigator, bookId)
            }

        }
    }
}


sealed class NavigationScene(val path: String, val title: String? = null) {
    object First : NavigationScene("/app", "first")
    object Welcome : NavigationScene("/app/welcome", "welcome")
    object Main : NavigationScene("/main", "main")
    object ChatTab : NavigationScene("/main/chat")
    object BookRead : NavigationScene("/main/cook/read")
    object TechTab : NavigationScene("/main/tech")
    object SettingsTab : NavigationScene("/main/settings")
}