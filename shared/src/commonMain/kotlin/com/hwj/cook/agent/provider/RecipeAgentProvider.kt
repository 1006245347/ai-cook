package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.ToolRegistry.Companion.invoke
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.agent.buildQwenLLMClient
import com.hwj.cook.agent.createMemoryProvider
import com.hwj.cook.agent.tools.RecipeSearchTool
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printLog

class RecipeAgentProvider : AgentProvider<String, String> {

    override var title: String = "Chef Agent"

    override val description: String = """
                   I'm a professional chef skilled in various cooking techniques. If the user's inquiry isn't related to cooking, I'll still provide a brief response. All replies will be clear and concise.
                    Your goal is to recommend recipes from the local knowledge base.
                    - When the user asks for something to eat, use tools to search recipes.
                    """.trimIndent()

    override suspend fun provideAgent(
        prompt: Prompt,
        onToolCallEvent: suspend (Message.Tool.Call) -> Unit,
        onToolResultEvent: suspend (Message.Tool.Result) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(buildQwenLLMClient(apiKey))


        val toolRegistry = ToolRegistry {
            tool(RecipeSearchTool)
        }
        val strategy = fastStrategy(onAssistantMessage)
        val agentConfig = AIAgentConfig(
            prompt = prompt, model = buildQwen3LLM("Qwen/Qwen2.5-7B-Instruct"),
            maxAgentIterations = 30
        )

        val userMemoryAgentProvider = createMemoryProvider()
        val agent = AIAgent.invoke(
            promptExecutor = remoteAiExecutor,
            strategy = strategy, agentConfig = agentConfig, toolRegistry = toolRegistry
        ) {

            install(AgentMemory) {
                this.memoryProvider = userMemoryAgentProvider
                this.productName = DATA_APPLICATION_NAME //设置产品名，为了范围对应
            }
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
                onLLMStreamingFrameReceived { context ->
                    when (val chunk = context.streamFrame) {
                        is StreamFrame.Append -> {
                            onLLMStreamFrameEvent(chunk.text)
                        }

                        is StreamFrame.ToolCall -> {

                        }

                        is StreamFrame.End -> {
                            printLog("\n[END] reason=${chunk.finishReason}")
                        }
                    }
                }
                onLLMStreamingFailed {
                    onErrorEvent("❌ Error: ${it.error}")
                }

                onAgentExecutionFailed { ctx -> //这个会返回给外部
                    onErrorEvent("failed1>${ctx.throwable.message}")
                }

                onAgentCompleted { ctx ->
                    // Skip finish event handling
//                    printD("agentCompleted>${ctx.result}")
                }
            }
        }
        return agent
    }


    fun fastStrategy(onAssistantMessage: suspend (String) -> String): AIAgentGraphStrategy<String, String> {
        return strategy<String, String>("rag-search") {
            //llm负责决定工具使用
            val nodeRequestLLM by nodeLLMRequestMultiple(name = "planner-llm")
            //执行工具
            val executeTools by nodeExecuteMultipleTools()
            //把工具结果喂给llm
            val sendToolResults by nodeLLMSendMultipleToolResults()

            // 输出LLM生成的消息到UI 回调 onAssistantMessage
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }

            edge(nodeStart forwardTo nodeRequestLLM)


            //返回的是联合类型
            edge( //nodeRequestLLM必须调用接口返回tool不然 executeTools就无法正常入参导致报错
                nodeRequestLLM forwardTo executeTools
                        onMultipleToolCalls { true }
            )

            edge( //适配不输出tool直接返回的情况
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            edge(executeTools forwardTo sendToolResults)
            edge(
                sendToolResults forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            edge(nodeAssistantMessage forwardTo nodeFinish)
        }
    }
}