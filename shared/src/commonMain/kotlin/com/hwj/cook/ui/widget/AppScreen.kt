package com.hwj.cook.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.CODE_IS_DARK
import com.hwj.cook.global.DATA_FIRST_WELCOME
import com.hwj.cook.global.DATA_USER_ID
import com.hwj.cook.global.NavigateRoute
import com.hwj.cook.global.NavigationScene
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.global.cSetBg
import com.hwj.cook.global.getCacheBoolean
import com.hwj.cook.global.saveString
import com.hwj.cook.models.FirstUiState
import com.hwj.cook.ui.viewmodel.MainVm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import moe.tlaster.precompose.koin.koinViewModel
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
    val isDark = remember { mutableStateOf(true) }
    LaunchedEffect(firstState) {
        subScope.launch {
            getCacheBoolean(DATA_FIRST_WELCOME, true).let {
                firstState.value = it
            }
            isDark.value = getCacheBoolean(CODE_IS_DARK, false)
        }
    }

    if (firstState.value) {
        navigator.navigate(NavigationScene.Welcome.path)
    } else {
        navigator.navigate(NavigationScene.Main.path)
    }
}

@Composable
fun WelcomeScreen(navigator: Navigator) {
    val subScope = rememberCoroutineScope()
    val isDark = remember { mutableStateOf(true) }
    LaunchedEffect(true) {
        subScope.launch {
            isDark.value = getCacheBoolean(CODE_IS_DARK, false)
//            saveBoolean(DATA_FIRST_WELCOME, true) //true每次都显示
            saveString(DATA_USER_ID, "888") //没有设计登录体系，换个userId缓存目录得变
            delay(1000)
            navigator.navigate(
                NavigationScene.Main.path, NavOptions(
                    popUpTo = PopUpTo.First()
                )
            )

        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "AI Cook", fontWeight = FontWeight.Bold, fontSize = 27.sp,
            color = cAutoTxt(isDark.value),
            modifier = Modifier.absolutePadding(top = 50.dp)
                .align(Alignment.Center)
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
            AppRoot()
        }
    }
}

/**
 * @author by jason-何伟杰，2026/1/22
 * des: 为了处理Android 首页背景色闪屏、白屏等
 */
@Composable
private fun AppRoot() {
    val subScope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf(FirstUiState()) }

    LaunchedEffect(Unit) {
        subScope.launch {
            val isDark = getCacheBoolean(CODE_IS_DARK, false)
            uiState = uiState.copy(isDarkBg = isDark)
        }
    }

    val bgColor by animateColorAsState(
        targetValue = when (uiState.isDarkBg) {
            null -> Color.Transparent
            true -> cSetBg(true)
            false -> cSetBg(false)
        }
    )

    val navigator = rememberNavigator()
    Box(Modifier.fillMaxSize().background(bgColor)) {
        NavigateRoute(navigator) //进Welcome
    }
}