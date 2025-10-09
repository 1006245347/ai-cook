package com.hwj.cook.android

import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.tooling.preview.Preview
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.ui.chat.ChatScreenContent
import java.util.ArrayList

/**
 * @author by jason-何伟杰，2025/10/9
 * des:kmp下无法在shared组件直接预览ui,放这里看吧
 */
@Preview //kmp的bug吧，无法在shared预览
@Composable
fun AgentDemoScreenPreview() {

    ThemeChatLite {
        ChatScreenContent(
            title = "Agent Demo",
            messages = mutableStateListOf(
                ChatMsg.SystemMsg("Hi, I'm an agent that can help you"),
                ChatMsg.UserMsg("Hello!"),
                ChatMsg.ToolCallMsg("Tool example, args {a=2, b=2}"),
                ChatMsg.ResultMsg("Result: 4"),
                ChatMsg.AgentMsg("Hello! How can I help you today?"),
                ChatMsg.ErrorMsg("Error: Something went wrong")
            ).toList(),
            inputTxt = "",
            isInputEnabled = true,
            isLoading = false,
            isChatEnded = false,
            onInputTxtChanged = {},
            onSendClicked = {},
            onRestartClicked = {},
            onNavigateBack = {}
        )
    }
}

@Preview
@Composable
fun AgentDemoScreenEndedPreview() {
    ThemeChatLite {
        ChatScreenContent(
            title = "Agent Demo",
            messages = mutableStateListOf(
                ChatMsg.SystemMsg("Hi, I'm an agent that can help you"),
                ChatMsg.UserMsg("Hello!"),
                ChatMsg.AgentMsg("Hello! How can I help you today?"),
                ChatMsg.SystemMsg("The agent has stopped.")
            ).toList(),
            inputTxt = "",
            isInputEnabled = false,
            isLoading = false,
            isChatEnded = true,
            onInputTxtChanged = {},
            onSendClicked = {},
            onRestartClicked = {},
            onNavigateBack = {}
        )
    }
}