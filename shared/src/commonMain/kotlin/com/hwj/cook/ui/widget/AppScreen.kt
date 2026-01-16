package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.CODE_IS_DARK
import com.hwj.cook.global.DATA_FIRST_WELCOME
import com.hwj.cook.global.NavigateRoute
import com.hwj.cook.global.NavigationScene
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.getCacheBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.navigation.NavOptions
import moe.tlaster.precompose.navigation.Navigator
import moe.tlaster.precompose.navigation.PopUpTo
import moe.tlaster.precompose.navigation.rememberNavigator
import org.koin.compose.KoinContext

/**
 * @author by jason-何伟杰，2025/9/18
 * des:应用的闪屏页，判断是直接进入主页还是显示欢迎页
 */
@Composable
fun FirstScreen(navigator: Navigator) {
    val subScope = rememberCoroutineScope()
    val firstState = remember { mutableStateOf(true) }

    LaunchedEffect(firstState) {
        subScope.launch {
            getCacheBoolean(DATA_FIRST_WELCOME, true).let {
                firstState.value = it
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (firstState.value) {
            navigator.navigate(NavigationScene.Welcome.path)
        } else {
            navigator.navigate(NavigationScene.Main.path)
        }
    }
}

@Composable
fun WelcomeScreen(navigator: Navigator) {
    val subScope = rememberCoroutineScope()
    val isDark = remember { mutableStateOf(false) }
    LaunchedEffect(true) {
        subScope.launch {
            isDark.value = getCacheBoolean(CODE_IS_DARK, false)
//            saveBoolean(DATA_FIRST_WELCOME, true) //true每次都显示
            delay(1000)
            navigator.navigate(
                NavigationScene.Main.path, NavOptions(
                    popUpTo = PopUpTo.First()
                )
            )

        }
    }

    Column(modifier = Modifier.fillMaxSize().background(cAutoBg())) {
        Text(
            text = "AI Cook", fontWeight = FontWeight.Bold, fontSize = 26.sp,
            color = cAutoTxt(isDark.value), modifier = Modifier.absolutePadding(top = 50.dp)
                .align(Alignment.CenterHorizontally)
        )
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