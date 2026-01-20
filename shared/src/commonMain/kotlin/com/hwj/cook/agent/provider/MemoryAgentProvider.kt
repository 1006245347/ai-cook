package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.createMemoryProvider
import com.hwj.cook.agent.memoryAgentConfig
import com.hwj.cook.agent.memoryStrategy
import com.hwj.cook.agent.tools.DiagnosticTool
import com.hwj.cook.agent.tools.UserInfoTool
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString

/**
 * @author by jason-何伟杰，2025/10/13
 * des:在tech模块获取用户偏好
 */
class MemoryAgentProvider(
    override var title: String = "Memory-Agent",
    override val description: String = "A conversational agent that supports long-term memory, with clear and concise responses."
) : AgentProvider<String, String> {

    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))
        val userMemoryAgentProvider = createMemoryProvider()

        val agent = AIAgent.invoke(
            promptExecutor = remoteAiExecutor,
            strategy = memoryStrategy(),
            agentConfig = memoryAgentConfig,
            toolRegistry = ToolRegistry{
                tool(UserInfoTool)
                tool(DiagnosticTool)
            }
        ) {
            install(AgentMemory.Feature) {
                this.memoryProvider = userMemoryAgentProvider
                this.productName = DATA_APPLICATION_NAME //设置产品名，为了范围对应
            }

            handleEvents {
                onToolCallStarting { ctx ->
                    onToolCallEvent("Tool ${ctx.toolName}, args ${ctx.toolArgs}")
                }

                onAgentExecutionFailed { ctx ->
                    onErrorEvent("${ctx.throwable.message}")
                }

                onAgentCompleted { ctx ->
                    // Skip finish event handling
                }
            }
        }
        return agent
    }
}