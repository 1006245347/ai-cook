package com.hwj.cook.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.ui.chat.ChatScreenContent
import com.hwj.cook.ui.chat.InputArea
import com.hwj.cook.ui.settings.SettingsScreenContent
import com.hwj.cook.ui.tech.TechScreenContent

/**
 * @author by jason-何伟杰，2025/10/9
 * des:kmp下无法在shared组件直接预览ui,放这里看吧
 */
//@Preview
@Composable
private fun TechScreenPreview(){
    ThemeChatLite {
        TechScreenContent(isDark = false,inputTxt = "ai", isInputEnabled = true, isLoading = false,
            isInputEnded = false,null,{},{})
    }
}

//@Preview //kmp的bug吧，无法在shared预览
@Composable
private fun AgentDemoScreenPreview() {

    ThemeChatLite {
        ChatScreenContent(
            title = "Agent Demo",
            messages = mutableListOf(
                ChatMsg.SystemMsg("Hi, I'm an agent that can help you"),
                ChatMsg.UserMsg("Hello!"),
                ChatMsg.ToolCallMsg("Tool example, args {a=2, b=2}"),
                ChatMsg.ResultMsg("Result: 4"),
                ChatMsg.AgentMsg("Hello! How can I help you today?"),
                ChatMsg.ErrorMsg("Error: Something went wrong")
            ),
            inputTxt = "",
            isInputEnabled = true,
            isLoading = false,
            isChatEnded = false,null,
            onInputTxtChanged = {},
            onSendClicked = {}, onStopChat = {},
            onRestartClicked = {},
            onNavigateBack = {}
        )
    }
}

//@Preview
@Composable
private fun AgentDemoScreenEndedPreview() {
    ThemeChatLite {
        ChatScreenContent(
            title = "Agent Demo",
            messages = mutableListOf(
                ChatMsg.SystemMsg("Hi, I'm an agent that can help you"),
                ChatMsg.UserMsg("Hello!"),
                ChatMsg.AgentMsg("Hello! How can I help you today?"),
                ChatMsg.SystemMsg("The agent has stopped.")
            ).toList(),
            inputTxt = "",
            isInputEnabled = false,
            isLoading = false,
            isChatEnded = true,null,
            onInputTxtChanged = {},
            onSendClicked = {}, onStopChat = {},
            onRestartClicked = {},
            onNavigateBack = {}
        )
    }
}

//@Preview
@Composable
private fun SettingScreenPreview(){
    ThemeChatLite {
        SettingsScreenContent(models=listOf(),isDark = false,{},{},{})
    }
}

@Preview
@Composable
private fun InputPreview(){
    InputArea("xxx",{},{},{},true,false, null,FocusRequester.Default)
}