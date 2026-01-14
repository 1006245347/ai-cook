package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.hwj.cook.agent.ChatSession
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cBlue244260FF
import com.hwj.cook.global.isDarkPanel
import com.hwj.cook.global.isLightPanel
import com.hwj.cook.global.printD
import com.hwj.cook.global.urlToImageAppIcon
import com.hwj.cook.ui.viewmodel.ChatVm
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
    val chatVm = koinViewModel(ChatVm::class)

    AppDrawerIn(
        navigator,
        drawerState,
        onConversationClicked,
        onNewConversationClicked,
        onThemeClicked,
        deleteConversation = { id ->
            chatVm.deleteSession(id, {
                //更新界面
            })
        },
        sessionList = chatVm.sessionState.collectAsState().value
    )
}

@Composable
fun AppDrawerIn(
    navigator: Navigator, drawerState: DrawerState, onConversationClicked: (String) -> Unit,
    onNewConversationClicked: () -> Unit,
    onThemeClicked: () -> Unit,
    deleteConversation: (String) -> Unit,
    sessionList: MutableList<ChatSession>, //agent
) {
    val mainVm = koinViewModel(MainVm::class)
    val canJump = remember { mutableStateOf(false) }
    val subScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))//影响键盘？

        DrawerHeader(onThemeClicked, drawerAction = {
            subScope.launch {
                drawerState.close()
            }
        })
        DividerItem(modifier = Modifier.padding(horizontal = 10.dp))

        NewSessionItem("New Chat", Icons.Outlined.AddComment) {
            onNewConversationClicked()
        }
        HistoryConversations(
            onConversationClicked,
            deleteConversation,
            sessionList
        )
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
    deleteConversation: (String) -> Unit,
    sessionList: MutableList<ChatSession>
) {
    val subScope = rememberCoroutineScope()
    val mainVm = koinViewModel(MainVm::class)
    val chatVm = koinViewModel(ChatVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val listState = rememberLazyListState()
    val sessionId by chatVm.currentSessionState.collectAsState()

    Box(Modifier.fillMaxWidth().weight(1f, false)) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            state = listState
        ) {
            //数据要倒序
            items(sessionList.toList()) { cell ->
                val mId = cell.id
                SessionItem(
                    text = cell.title,
                    Icons.AutoMirrored.Filled.Message,
                    selected = mId == sessionId,
                    onChatClicked = {
                        printD("click-$mId")
                        if (sessionId != mId) {
                            onConversationClicked(mId)
                            subScope.launch {
                                chatVm.loadSessionById(mId)
                            }
                        }
                    },
                    onDeleteClicked = { deleteConversation(mId) })
            }
        }
    }
}

@Composable
private fun NewSessionItem(
    text: String,
    icon: ImageVector = Icons.Filled.Edit,
    onChatClicked: () -> Unit
) {
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(CircleShape)
            .clickable(onClick = onChatClicked),
        verticalAlignment = CenterVertically
    ) {
        val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
        Icon(
            icon,
            tint = iconTint,
            modifier = Modifier
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp)
                .size(25.dp),
            contentDescription = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, //其实Android15也是没有效果,
            modifier = Modifier.padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
private fun SessionItem(
    text: String,
    icon: ImageVector = Icons.Filled.Edit,
    selected: Boolean,
    onChatClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val chatVm = koinViewModel(ChatVm::class)
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val isStopReceiveState = chatVm.stopReceivingState.collectAsState().value
    val background = if (selected) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(30))
            .then(background)
            .clickable(onClick = onChatClicked),
        verticalAlignment = CenterVertically
    ) {
        val iconTint = if (selected) {
            if (isDark) {
                isDarkPanel()
            } else {
                isLightPanel()
            }
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Icon(
            icon,
            tint = iconTint,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                .size(25.dp),
            contentDescription = null,
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) {
                cAutoTxt(isDark)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .padding(start = 12.dp)
                .fillMaxWidth(0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected && !isStopReceiveState) {
            //防止删除正在生成的会话
        } else {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(
                    end = 5.dp
                ).size(35.dp).clickable { onDeleteClicked() }
            )
        }
    }
}

@Composable
fun DividerItem(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}