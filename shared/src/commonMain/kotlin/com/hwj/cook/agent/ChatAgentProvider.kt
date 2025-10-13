package com.hwj.cook.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString

/**
 * @author by jason-何伟杰，2025/10/11
 * des:问答对话只需要会话消息和大模型接口即可，不需要完整的智能体能力，
 * 这里写个简单的问答智能体用来实现简单的问答对话
 */
class ChatAgentProvider(
    override var title: String = "Chat",
    override val description: String="A conversational agent that supports long-term memory, with clear and concise responses."
) : AgentProvider {
    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        val agent = AIAgent(
            promptExecutor = remoteAiExecutor,
            systemPrompt = "Hi,I'm a Chef agent",
            llmModel = OpenAIModels.Chat.GPT4o,
        )
        return agent
    }
}