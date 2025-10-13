package com.hwj.cook.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.hwj.cook.agent.tools.RecipeTools
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString

/**
 * @author by jason-何伟杰，2025/10/10
 * des: Agent for cook
 */
class AICookAgentProvider : AgentProvider {
    override var title: String = "Chef Agent"

    override val description: String = "Hi,I'm a chef agent,I can teach how to cook."

    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        val toolRegistry = ToolRegistry {
            tool(RecipeTools.SearchRecipeTool)
            tool(RecipeTools.UserFlavorTool)
            tool(ExitTool)
        }

        val strategy = strategy(title) {
            // 发送LLM请求（可生成多个工具调用或回复） LLM分析用户问题，决定是否调用工具
            val nodeRequestLLM by nodeLLMRequestMultiple()

//            输出LLM生成的消息到UI 回调 onAssistantMessage
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }

//            并行执行多个工具调用 比如计算加减乘除
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)

// 把工具执行结果反馈给LLM, LLM可能会再次推理或继续计算
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()

//            压缩LLM上下文 ,当消息过多时减少历史负担
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            //开始 LLM请求
            edge(nodeStart forwardTo nodeRequestLLM)

//            当LLM输出包含多个tool call时，进入工具执行节点。
            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                        onMultipleToolCalls { true }
            )

//            如果LLM输出的是文本消息（assistant回复），就走到消息节点。
            edge(
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            //再决策or结束  消息回路  当用户继续输入新内容，重新触发LLM处理。
            edge(nodeAssistantMessage forwardTo nodeRequestLLM)

            // Finish condition - if exit tool is called, go to nodeFinish with tool call result.
//            调用了退出工具（ExitTool） → 结束
            edge(
                nodeExecuteToolMultiple forwardTo nodeFinish
                        onCondition { it.singleOrNull()?.tool == ExitTool.name }
                        transformed { it.single().result!!.toString() }
            )

//            上下文太大 → 压缩再反馈
            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                        onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

//            上下文正常 → 直接反馈工具结果给LLM
            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                        onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            //工具结果反馈后
            //再次决策回路 ，再次生成新的工具调用(形成循环）
            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                        onMultipleToolCalls { true }
            )

            //输出文字结果给用户
            edge(
                nodeSendToolResultMultiple forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )
        }

        val agentConfig = AIAgentConfig(
            prompt = prompt("cook") {
                system(
                    """
                    You are a smart recipe assistant.
                    Your goal is to recommend recipes from the local knowledge base.
                    - When the user asks for something to eat, use tools to search recipes.
                    - Filter based on user's dislikes or dietary needs.
                    - Finally recommend one or more recipes.
                    """.trimIndent()
                )
            }, model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 10
        )

        val agent = AIAgent(
            promptExecutor = remoteAiExecutor,
            strategy = strategy, agentConfig = agentConfig, toolRegistry = toolRegistry
        ) {
            handleEvents {
                onToolCallStarting { ctx ->
                    onToolCallEvent("Tool ${ctx.tool.name}, args ${ctx.toolArgs}")
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