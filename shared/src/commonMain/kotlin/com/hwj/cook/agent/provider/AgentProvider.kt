package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent

//智能体实现基础
interface AgentProvider<I, O> {

    var title: String
    val description: String

    suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<I, O>
}

//智能体的包装类
data class AgentInfoCell(var name: String, val isSupport: Boolean = true)
