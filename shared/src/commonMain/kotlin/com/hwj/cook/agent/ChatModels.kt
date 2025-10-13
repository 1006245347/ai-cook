@file:OptIn(ExperimentalUuidApi::class)

package com.hwj.cook.agent

import com.hwj.cook.global.getMills
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ChatModels(
    val id: String = Uuid.random().toString(),
    val title: String = "新会话",
    val createTime: Long = getMills(),
    val messages: MutableList<ChatMsg>
)

@OptIn(ExperimentalUuidApi::class)
@Serializable
sealed class ChatMsg {
    val id: String = Uuid.random().toString()
    val createTime: Long =0L

    data class UserMsg(val txt: String) : ChatMsg()
    data class AgentMsg(val txt: String) : ChatMsg()
    data class SystemMsg(val txt: String?) : ChatMsg()
    data class ErrorMsg(val txt: String?) : ChatMsg()
    data class ToolCallMsg(val txt: String?) : ChatMsg()
    data class ResultMsg(val txt: String) : ChatMsg()
}