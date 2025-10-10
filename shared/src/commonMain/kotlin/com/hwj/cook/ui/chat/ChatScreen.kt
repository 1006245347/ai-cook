package com.hwj.cook.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.dp10
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

    val sessionId = "ss"
    LaunchedEffect(sessionId) {
        subScope.launch {
            chatVm.createAgent()
        }
    }

    ChatScreenContent(
        title = uiObs.title,
        messages = uiObs.messages,
        inputTxt = uiObs.inputTxt,
        isInputEnabled = uiObs.isInputEnabled,
        isLoading = uiObs.isLoading,
        isChatEnded = uiObs.isChatEnded,
        onInputTxtChanged = chatVm::updateInputText,
        onSendClicked = chatVm::sendMessage,
        onRestartClicked = chatVm::restartChat,
        onNavigateBack = {
//        navigator.goBack()
        })
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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
            state = listState, verticalArrangement = Arrangement.spacedBy(3.dp)
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
        if (isChatEnded) {
            RestartButton(onRestartClicked)
        } else {
            InputArea(
                text = inputTxt, onTextChanged = onInputTxtChanged,
                onSendClicked = {
                    onSendClicked()
                    focusManager.clearFocus()
                }, isEnabled = isInputEnabled,
                isLoading = isLoading,
                focusRequester = focusRequester
            )
        }
    }
}