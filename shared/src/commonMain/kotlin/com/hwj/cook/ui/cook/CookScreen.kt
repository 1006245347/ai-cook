package com.hwj.cook.ui.cook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.NavigationScene
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cLightLine
import com.hwj.cook.global.loadingTip
import com.hwj.cook.global.onlyDesktop
import com.hwj.cook.global.printD
import com.hwj.cook.models.BookNode
import com.hwj.cook.ui.viewmodel.CookVm
import com.hwj.cook.ui.viewmodel.MainVm
import io.ktor.http.encodeURLQueryComponent
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

/**
 * @author by jason-何伟杰，2025/9/28
 * des:菜谱页面Tab
 */
@Composable
fun CookScreen(navigator: Navigator) {
    val mainVm = koinViewModel(MainVm::class)
    val cookVm = koinViewModel(CookVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val bookRootState by cookVm.bookRootState.collectAsState()
    var initialized by rememberSaveable { mutableStateOf(false) }
    val expendNodeList by cookVm.expendNodeState.collectAsState()

    //保证重组也执行一次初始化
    LaunchedEffect(initialized) {
        if (!initialized) {
            cookVm.initialize()
            initialized = true
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            if (bookRootState.isLoading) {
                Text(loadingTip, color = MaterialTheme.colorScheme.secondary)
            } else if (bookRootState.error != null) {
                Text(bookRootState.error!!)
            } else {
                bookRootState.data?.let { root ->
                    LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                        //第一层只有一个文件夹，直接拿它的子类
                        root.children.forEach { cell ->
                            item {
                                BookNodeView(
                                    navigator,
                                    cell,
                                    0,
                                    isDark,
                                    onStart = { path ->
                                        path in expendNodeList
                                    }, onToggle = { path ->
                                        cookVm.toggleExpand(path)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookNodeView(
    navigator: Navigator,
    node: BookNode,
    level: Int = 0,
    isDark: Boolean,
    onStart: (String) -> Boolean,
    onToggle: (String) -> Unit
) {
    val expanded = onStart(node.realPath)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        onToggle(node.realPath) //将数据更新处理交给vm记录
                    } else {
                        if (node.name.contains(".md")) {
                            val argPath = node.realPath.encodeURLQueryComponent(encodeFull = true)
//                            printLog("encode>$argPath")//encodeFull把 /都编码了不然导航会报错
                            if (onlyDesktop()) {
                                printD("jump>${NavigationScene.BookRead.path}/$argPath")
                                navigator.navigate(NavigationScene.BookRead.path + "/$argPath")
                            } else {
                                navigator.navigate(NavigationScene.BookRead.path + "/$argPath")
                            }
                        }
                    }
                }.padding(start = (level * 16).dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                Text(if (expanded) "\uD83D\uDCC2" else "\uD83D\uDCC2")
            } else {
                Text("\uD83D\uDCD6")
            }
            Spacer(Modifier.width(8.dp))
            Text(node.name, fontSize = 17.sp, color = cAutoTxt(isDark))
        }
        HorizontalDivider(thickness = 1.dp, color = cLightLine())
        if (expanded && node.isDirectory) {
            node.children.forEach { child ->
                BookNodeView(
                    navigator,
                    child,
                    level + 1,
                    isDark,
                    onStart = onStart,
                    onToggle = onToggle
                )
            }
        }
    }
}