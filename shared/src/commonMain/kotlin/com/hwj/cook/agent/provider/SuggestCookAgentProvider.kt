package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.platformAgentTools

class SuggestCookAgentProvider(
    override var title: String = "",
    override val description: String = "You are a agent,you're responsible for running a Switch and perform operations on it by request"
) : AgentProvider<String, List<Message.Response>> {

    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, List<Message.Response>> {

        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        val toolRegistry = ToolRegistry {}.plus(platformAgentTools())


        val agent = openAiAgent(toolRegistry, remoteAiExecutor) {
            handleEvents {
                onToolCallStarting { context ->
                    println("\n🔧 Using ${context.tool.name} with ${context.toolArgs}... ")
                }
                onLLMStreamingFrameReceived { context ->
                    (context.streamFrame as? StreamFrame.Append)?.let { frame ->
                        print(frame.text)
                    }
                }
                onLLMStreamingFailed {
                    println("❌ Error: ${it.error}")
                }
                onLLMStreamingCompleted {
                    println("")
                }
            }
        }
//            agent.run("")


        //得到agent的类型是 GraphAIAgent<String, List<Message.Response>>
        return agent //报错 无法对应函数的 GraphAIAgent<String, String>
    }


    //Argument type mismatch: actual type is 'AIAgentGraphStrategy<String, List<Message.Response>>',
    // but 'AIAgentGraphStrategy<String, String>' was expected.
    private fun openAiAgent(
        toolRegistry: ToolRegistry,
        executor: PromptExecutor,
        installFeatures: GraphAIAgent.FeatureContext.() -> Unit = {}
    ) = AIAgent(
        promptExecutor = executor,
        strategy = streamingWithToolsStrategy(),
        llmModel = OpenAIModels.Chat.GPT4o,
        systemPrompt = "You're responsible for running a Switch and perform operations on it by request",
        temperature = 0.0,
        toolRegistry = toolRegistry,
        installFeatures = installFeatures
    )

    fun streamingWithToolsStrategy() = strategy("streaming_loop") {
        val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
        val nodeStreaming by nodeLLMRequestStreamingAndSendResults() // 返回 List<Message.Response>

        val mapStringToRequests by node<String, List<Message.Request>> { input ->
            listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
        }

        val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input ->
            llm.writeSession {
                appendPrompt {
                    input.filterIsInstance<Message.User>()
                        .forEach {
                            user(it.content)
                        }

                    tool {
                        input.filterIsInstance<Message.Tool.Result>()
                            .forEach {
                                result(it)
                            }
                    }
                }
                input
            }
        }

        val mapToolCallsToRequests by node<List<ReceivedToolResult>, List<Message.Request>> { input ->
            input.map { it.toMessage() }
        }

        edge(nodeStart forwardTo mapStringToRequests)
        edge(mapStringToRequests forwardTo applyRequestToSession)
        edge(applyRequestToSession forwardTo nodeStreaming)
        edge(nodeStreaming forwardTo executeMultipleTools onMultipleToolCalls { true })
        edge(executeMultipleTools forwardTo mapToolCallsToRequests)
        edge(mapToolCallsToRequests forwardTo applyRequestToSession)
        edge(
            nodeStreaming forwardTo nodeFinish onCondition {
                it.filterIsInstance<Message.Tool.Call>().isEmpty() //不再调用工具，就输出noteStreaming
            }
        )
    }
}