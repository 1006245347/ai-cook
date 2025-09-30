package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cBlue244260FF
import com.hwj.cook.global.urlToImageAppIcon
import com.hwj.cook.ui.viewmodel.ConversationViewModel
import com.hwj.cook.ui.viewmodel.MainVm
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun AppDrawer(
    navigator: Navigator, drawerState: DrawerState, onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    onThemeClicked: () -> Unit
) {
    AppDrawerIn(
        navigator, drawerState, onConversationClicked, onNewConversationClicked, onThemeClicked,
        deleteConversation = { id -> }, sessionList = null
    )
}

@Composable
fun AppDrawerIn(
    navigator: Navigator, drawerState: DrawerState, onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    onThemeClicked: () -> Unit,
    deleteConversation: (String) -> Unit,
    sessionList: MutableList<String>?, //agent
) {
    val mainVm = koinViewModel(MainVm::class)
    val canJump = remember { mutableStateOf(false) }
    val subScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))//影响键盘？

        DrawerHeader(onThemeClicked, drawerAction = {
            subScope.launch {
                drawerState.close()
            }
        })
        Column(Modifier.height(100.dp).background(color = cAutoBg())) {
            Text("Drawer>>>", fontSize = 50.sp, color = cBlue244260FF())
        }
    }
}

@Composable
private fun DrawerHeader(onThemeClicked: () -> Unit, drawerAction: () -> Unit = {}) {

    val paddingSizeModifier = Modifier
        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
        .size(34.dp)
    Row(verticalAlignment = CenterVertically, horizontalArrangement = SpaceBetween) {
        Row(
            modifier = Modifier
                .padding(13.dp)
                .weight(1f), verticalAlignment = CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(urlToImageAppIcon)
                    .crossfade(true).build(),
                contentDescription = "Header",
                modifier = paddingSizeModifier.then(Modifier.clip(RoundedCornerShape(6.dp))),
                contentScale = ContentScale.Crop,
//                error = painterResource(Res.drawable.ic_big_logo),
            )

            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    "AI Cook",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor
                )
                Text(
                    "Powered by KMP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = PrimaryColor,
                )
            }
        }

        IconButton(
            onClick = {
                onThemeClicked.invoke()
            }, modifier = Modifier.padding(bottom = 40.dp)
        ) {
            Icon(
                Icons.Filled.WbSunny,
                "dark",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        IconButton(
            onClick = {
                drawerAction.invoke()
            }, modifier = Modifier.padding(bottom = 40.dp)
        ) {
            Icon(
                Icons.Filled.OpenInFull,
                "drawer",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

    }
}

@Composable
private fun ColumnScope.HistoryConversations(
    onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    deleteConversation: (String) -> Unit,
    sessionList: MutableList<String>?
) {
    val subScope = rememberCoroutineScope()
    val mainVm = koinViewModel(MainVm::class)
    val conversationVm = koinViewModel(ConversationViewModel::class)
    val isDark = mainVm.darkState.collectAsState().value
    val listState = rememberLazyListState()
//    val curChatId by conversationVm.currentConversationState.collectAsState()
}