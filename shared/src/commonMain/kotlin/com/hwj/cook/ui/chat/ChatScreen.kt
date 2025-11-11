package com.hwj.cook.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.cGreyF0F0F0
import com.hwj.cook.global.dp10
import com.hwj.cook.global.onlyDesktop
import com.hwj.cook.ui.viewmodel.ChatVm
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

/**
 * @author by jason-何伟杰，2025/10/øø9
 * des:聊天界面
 */
@Composable
fun ChatScreen(navigator: Navigator) {
    val subScope = rememberCoroutineScope()
    val chatVm = koinViewModel(ChatVm::class)
    val uiObs by chatVm.uiObs.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val sessionId = chatVm.currentSessionState.collectAsState()
    LaunchedEffect(sessionId) {
        subScope.launch {
            chatVm.createAgent()
            chatVm.loadAskSession()
        }
    }

    Box(Modifier.fillMaxSize()) {
        ChatScreenContent(
            title = uiObs.title,
            messages = uiObs.messages,
            inputTxt = uiObs.inputTxt,
            isInputEnabled = uiObs.isInputEnabled,
            isLoading = uiObs.isLoading,
            isChatEnded = uiObs.isChatEnded,
            onInputTxtChanged = chatVm::updateInputText,
            onSendClicked = chatVm::sendMessage,
            onRestartClicked = chatVm::restartRun,
            onNavigateBack = {
//        navigator.goBack()
            })

//    if (!showPermissionDialog) {
//        createPermission(PermissionPlatform.STORAGE, grantedAction = {
//            showPermissionDialog = false
//        }, deniedAction = {
//            showPermissionDialog = false
//        })
//    }
    }
}

@Composable
fun ChatScreenContent(
    title: String?,
    messages: List<ChatMsg>,
    inputTxt: String,
    isInputEnabled: Boolean,
    isLoading: Boolean,
    isChatEnded: Boolean,
    onInputTxtChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onRestartClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    //桌面端无列表时输入框居中，其他在底下
//    val isMiddle = messages.isEmpty() && onlyDesktop()
    val isMiddle = messages.isEmpty()
    //页面切换回来保持输入内容  inputTxt
    val inputCache = rememberSaveable { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = if (isMiddle) Arrangement.Center else Arrangement.Top,
        horizontalAlignment = if (isMiddle) Alignment.CenterHorizontally else Alignment.Start
    ) {

        MessageList(
            Modifier.weight(1f).padding(horizontal = 16.dp).background(cGreyF0F0F0()),
            messages
        )
        if (isChatEnded) {
            RestartButton(onRestartClicked)
        } else {
            InputArea(
                text = inputTxt, onTextChanged = onInputTxtChanged,
                onSendClicked = { //触发模型功能
                    onSendClicked()
                    focusManager.clearFocus()
                }, isEnabled = isInputEnabled,
                isLoading = isLoading,
                focusRequester = focusRequester
            )
        }
    }
}

@Composable
private fun MessageList(modifier: Modifier, messages: List<ChatMsg>) {
    Box(
        modifier = modifier
    ) {
        LazyColumn(
            contentPadding =
                WindowInsets.statusBars.add(WindowInsets(top = 90.dp)).asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
            state = rememberLazyListState(), verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(messages) { msg ->
                when (msg) {
                    is ChatMsg.UserMsg -> UserMessageBubble(msg.txt)
                    is ChatMsg.AgentMsg -> AgentMessageBubble(msg.txt)
                    is ChatMsg.SystemMsg -> msg.txt?.let { SystemMessageItem(it) }
                    is ChatMsg.ErrorMsg -> msg.txt?.let { ErrorMessageItem(it) }
                    is ChatMsg.ToolCallMsg -> msg.txt?.let { ToolCallMessageItem(it) }
                    is ChatMsg.ResultMsg -> ResultMessageItem(msg.txt)
                }
            }

            item { Spacer(Modifier.height(dp10())) }
        }
    }
}