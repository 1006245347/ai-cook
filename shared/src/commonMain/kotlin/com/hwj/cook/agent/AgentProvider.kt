package com.hwj.cook.agent

import ai.koog.agents.core.agent.AIAgent

interface AgentProvider {

    var title: String
    val description: String

    suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> Unit
    ): AIAgent<String, String>
}