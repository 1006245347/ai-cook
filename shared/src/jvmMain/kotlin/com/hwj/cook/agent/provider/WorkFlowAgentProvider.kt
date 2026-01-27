package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.agent.buildQwenLLMClient
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.getCacheString

/**
 * @author by jason-何伟杰，2026/1/22
 * des:验证某些功能的智能体
 */
class WorkFlowAgentProvider(
    override var title: String = "workAgent",
    override val description: String = "I'm an assistant who provides simple and clear answers to users."
) : AgentProvider<String, String> {
    override suspend fun provideAgent(
        prompt: Prompt,
        onToolCallEvent: suspend (Message.Tool.Call) -> Unit,
        onToolResultEvent: suspend (Message.Tool.Result) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        val mcpKey = getCacheString(DATA_MCP_KEY)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }

        val executor = SingleLLMPromptExecutor(buildQwenLLMClient(apiKey))
        return AIAgent.invoke(
            promptExecutor = executor,
            agentConfig = AIAgentConfig(
                prompt = prompt,
                model = buildQwen3LLM(), maxAgentIterations = 50
            )
        )
    }
}