package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.message.Message

//智能体实现基础
interface AgentProvider<I, O> {

    var title: String
    val description: String

    suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onLLMStreamFrameEvent:suspend (String)-> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<I, O>
}

//智能体的包装类
data class AgentInfoCell(var name: String, val isSupport: Boolean = true)

//koin无法直接在构建list的类型，这里再包裹一层
data class ResponseStreamList(val items: List<Message.Response>)