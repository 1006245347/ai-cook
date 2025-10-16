@file:OptIn(ExperimentalUuidApi::class)

package com.hwj.cook.agent

import com.hwj.cook.global.getMills
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

//多轮问答构成一组会话，智能体本质是多轮会话的上下文
@Serializable
data class ChatModels(
    val id: String = Uuid.random().toString(),
    val title: String = "新会话",
    val createTime: Long = getMills(),
    val messages: MutableList<ChatMsg>
)

//单条消息
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

//配置是为了序列化某个实体时，可以忽略错误继续执行
val JsonApi = Json {
    isLenient=true
    ignoreUnknownKeys=true
}