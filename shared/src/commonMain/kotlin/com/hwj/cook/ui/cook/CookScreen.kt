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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwj.cook.global.loadingTip
import com.hwj.cook.models.BookNode
import com.hwj.cook.ui.viewmodel.CookVm
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

/**
 * @author by jason-何伟杰，2025/9/28
 * des:菜谱页面Tab
 */
@Composable
fun CookScreen(globalNavigator: Navigator, insideNavigator: Navigator) {

    val cookVm = koinViewModel(CookVm::class)
    val bookRootState by cookVm.bookRootState.collectAsState()
    var initialized by remember { mutableStateOf(false) }
    val subScope = rememberCoroutineScope()
    //保证重组也执行一次初始化
    LaunchedEffect(initialized) {
        if (!initialized) {
            cookVm.initialize()
            initialized = true
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (bookRootState.isLoading) {
                Text(loadingTip, color = MaterialTheme.colorScheme.secondary)
            } else if (bookRootState.error != null) {
                Text(bookRootState.error!!)
            } else {
                bookRootState.data?.let { root ->
                    LazyColumn {
                        item {
                            BookNodeView(root,1)
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun BookNodeView(node: BookNode, level: Int = 0) {
    var expanded by remember { mutableStateOf(false) }
    Column {

        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable {
                    if (node.isDirectory) expanded = !expanded
                }.padding(start = (level * 16).dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                Text(if (expanded) "\uD83D\uDCC2" else "\uD83D\uDCC2")
            } else {
                Text("\uD83D\uDCD6")
            }
            Spacer(Modifier.width(8.dp))
            Text(node.name)
        }

        if (expanded && node.isDirectory) {
            node.children.forEach { child ->
                BookNodeView(child, level + 1)
            }
        }
    }
}