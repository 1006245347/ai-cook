package com.hwj.cook.ui.widget

import androidx.compose.runtime.Composable
import com.hwj.cook.data.local.ResParse
import com.hwj.cook.global.NavigateRoute
import com.hwj.cook.global.globalScope
import com.hwj.cook.loadZipRes
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.rememberNavigator
import org.koin.compose.KoinContext

@Composable
fun  FirstScreen(navigator: Navigator) {

    globalScope.launch {

//    ResParse.loadRecipe()
        loadZipRes()
    }
}

/**
 * des:通过路由进入APP
 */
@Composable
fun PlatformAppStart() {
    PreComposeApp {
        KoinContext {
            val navigator = rememberNavigator()
            NavigateRoute(navigator) //进Welcome
        }
    }
}