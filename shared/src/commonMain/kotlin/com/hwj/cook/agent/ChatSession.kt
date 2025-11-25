@file:OptIn(ExperimentalUuidApi::class)

package com.hwj.cook.agent

import com.hwj.cook.global.getMills
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

//多轮问答对话构成一组会话，智能体本质是多轮会话的上下文，分清对话和会话！！！
@Serializable
data class ChatSession(
    val id: String = Uuid.random().toString(),
    val title: String = "新会话",
    val createTime: Long = getMills(),
//    val messages: MutableList<ChatMsg>
)

//单条消息对话
@OptIn(ExperimentalUuidApi::class)
@Serializable
sealed class ChatMsg {
    val id: String = Uuid.random().toString()
    var sessionId: String=""
    val createTime: Long = 0L
    var state: ChatState = ChatState.Idle

    data class UserMsg(val txt: String) : ChatMsg()
    data class AgentMsg(val txt: String) : ChatMsg()
    data class SystemMsg(val txt: String?) : ChatMsg()
    data class ErrorMsg(val txt: String?) : ChatMsg()
    data class ToolCallMsg(val txt: String?) : ChatMsg()
    data class ResultMsg(val txt: String) : ChatMsg()
}

@Serializable
sealed class ChatState {
    object Idle : ChatState()
    object Thinking : ChatState()
    object ToolCalling : ChatState()
    object Responding : ChatState()
    data class Completed(val text: String?) : ChatState()
    data class Error(val message: String?) : ChatState()
}

//配置是为了序列化某个实体时，可以忽略错误继续执行
val JsonApi = Json {
    isLenient = true
    ignoreUnknownKeys = true
    //    encodeDefaults = true
//        prettyPrint = true
//        coerceInputValues = true
}