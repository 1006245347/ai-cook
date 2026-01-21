package com.hwj.cook.models

import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.defSystemTip

/**
 * @author by jason-何伟杰，2025/9/18
 * des: 把所有状态声明放这里
 */
data class AppConfigState(
    val isLoading: Boolean = false,
    val data: BookNode? = null,
    val error: String? = null
)

data class BookConfigState(
    val isLoading: Boolean = false,
    val data: BookNode? = null,
    val error: String? = null
)

data class AgentUiState(
    val title: String? = "Agent",
    val messages: List<ChatMsg> = listOf(ChatMsg.SystemMsg(defSystemTip)),
    val inputTxt: String = "",
    val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isChatEnded: Boolean = false,

    // For handling user responses when agent asks a question
    val userResponseRequested: Boolean = false,
    val currentUserResponse: String? = null
)

data class MemoryUiState(
    val inputTxt: String = "", val isInputEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val memoryOfUser: String?=null
)

//AVI 意图区别，项目小，为了方便只分两大类UI、Data.
sealed class AppIntent {
    //UI处理 明亮、黑暗主题切换
    data object ThemeSetIntent : AppIntent()

    //数据处理
    data object BookLoadIntent : AppIntent()
}