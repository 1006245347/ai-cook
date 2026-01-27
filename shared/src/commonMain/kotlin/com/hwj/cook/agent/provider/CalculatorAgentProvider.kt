package com.hwj.cook.agent.provider

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
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.agent.tools.CalculatorTools
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printLog


/**
 * @author by jason-何伟杰，2025/12/4
 * des: demo
 */
class CalculatorAgentProvider() : AgentProvider<String, String> {

    override var title: String = "Calculator"
    override val description: String = """
                    You are a calculator.
                    You will be provided math problems by the user.
                    Use tools at your disposal to solve them.
                    Provide the answer and ask for the next problem until the user asks to stop.
                    """.trimIndent()

    override suspend fun provideAgent(
        prompt: Prompt,
        onToolCallEvent: suspend (Message.Tool.Call) -> Unit,
        onToolResultEvent: suspend (Message.Tool.Result) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent<String, String> {
//        val (llmClient, model) = provideLLMClient.invoke()
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val executor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        // Create tool registry with calculator tools
        val toolRegistry = ToolRegistry {
            tool(CalculatorTools.PlusTool)
            tool(CalculatorTools.MinusTool)
            tool(CalculatorTools.DivideTool)
            tool(CalculatorTools.MultiplyTool)

            tool(ExitTool)
        }

        @Suppress("DuplicatedCode") val strategy = strategy(title) {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            edge(nodeStart forwardTo nodeRequestLLM)

            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple onMultipleToolCalls { true })

            edge(nodeRequestLLM forwardTo nodeAssistantMessage transformed { it.first() } onAssistantMessage { true })

            edge(nodeAssistantMessage forwardTo nodeRequestLLM)

            // Finish condition - if exit tool is called, go to nodeFinish with tool call result.
            edge(nodeExecuteToolMultiple forwardTo nodeFinish onCondition { it.singleOrNull()?.tool == ExitTool.name } transformed { it.single().result!!.toString() })

            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory) onCondition { _ -> llm.readSession { prompt.messages.size > 100 } })

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple) onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } })

            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple) onMultipleToolCalls { true })

            edge(nodeSendToolResultMultiple forwardTo nodeAssistantMessage transformed { it.first() } onAssistantMessage { true })
        }

        // Create agent config with proper prompt
        val agentConfig = AIAgentConfig(
            prompt = prompt, //计算器这个strategy一轮中会有反问，所以没有最后end都不是新的prompt构建
            model = buildQwen3LLM(), maxAgentIterations = 50
        )

        // Return the agent
        return AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
        ) {
            handleEvents {
                onToolCallStarting { ctx ->
//                    onToolCallEvent("\n🔧 Using ${ctx.toolName} with ${ctx.toolArgs}... ")
                    onToolCallEvent(
                        Message.Tool.Call(
                            id = ctx.toolCallId,
                            tool = ctx.toolName,
                            part = ContentPart.Text(text = JsonApi.encodeToString(ctx.toolArgs)),
                            metaInfo = ResponseMetaInfo.Empty //对不上类型
                        )
                    )
                }
                onToolCallCompleted { ctx ->
                    onToolResultEvent(
                        Message.Tool.Result(
                            id = ctx.toolCallId,
                            tool = ctx.toolName,
                            part = ContentPart.Text(text = JsonApi.encodeToString(ctx.toolArgs)),
                            metaInfo = RequestMetaInfo.Empty
                        )
                    )
                }
                onLLMStreamingFrameReceived { ctx ->
                    when (val chunk = ctx.streamFrame) {
                        is StreamFrame.Append -> {
                            onLLMStreamFrameEvent(chunk.text)
                        }

                        is StreamFrame.ToolCall -> {
                            printLog("Tool call:${chunk.name} args=${chunk.content} ")
                        }

                        is StreamFrame.End -> {
                            printLog("\n[END] reason=${chunk.finishReason}")
                        }
                    }
                }

                onAgentExecutionFailed { ctx ->
                    onErrorEvent("${ctx.throwable.message}")
                }

                onAgentCompleted { ctx ->
                    printD("onAgentCompleted>${ctx.result}")
                    // Skip finish event handling
                }
            }
        }
    }
}