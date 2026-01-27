package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentService
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.invoke
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printLog
import com.hwj.cook.platformAgentTools
import io.ktor.client.request.invoke

class SuggestCookAgentProvider(
    override var title: String = "SwitchAgent",
    override val description: String = "You are a agent,you're responsible for running a Switch and perform operations on it by request"
) : AgentProvider<String, String> {

    override suspend fun provideAgent(prompt: Prompt,
        onToolCallEvent: suspend (Message.Tool.Call) -> Unit,
        onToolResultEvent: suspend (Message.Tool.Result) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {

        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        val toolRegistry = ToolRegistry {}.plus(platformAgentTools())

        val agent = openAiAgent(toolRegistry,prompt, remoteAiExecutor) {
            handleEvents {   //可流式的关键
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
                    //可细分帧类型数据
//                    (context.streamFrame as? StreamFrame.Append)?.let { frame ->
////                        print(frame.text)
//                        onLLMStreamFrameEvent(frame.text)
//                    }
                }
                onLLMStreamingFailed {
//                    println("❌ Error: ${it.error}")
                    onErrorEvent("❌ Error: ${it.error}")
                }
                onLLMStreamingCompleted {
//                    println("")
                }
                //还有好多回调callback可用
            }
        }


        //是可以考虑用bean-> json的方式？


        //得到agent的类型是 openAiAgent return  GraphAIAgent<String, List<Message.Response>>
        return agent //error  agent type is  GraphAIAgent<String, String>
    }


    //Argument type mismatch: actual type is 'AIAgentGraphStrategy<String, List<Message.Response>>',
    // but 'AIAgentGraphStrategy<String, String>' was expected.
    fun openAiAgent(
        toolRegistry: ToolRegistry,prompt: Prompt,
        executor: PromptExecutor,
        installFeatures: GraphAIAgent.FeatureContext.() -> Unit = {}
    ) = AIAgent.Companion.invoke(
        promptExecutor = executor,
        strategy = streamingWithToolsStrategy(),
        agentConfig = AIAgentConfig(prompt = prompt, model = buildQwen3LLM(), maxAgentIterations = 30),
        toolRegistry = toolRegistry,
        installFeatures = installFeatures
    )


    fun streamingWithToolsStrategy() = strategy("streaming_loop") {

        val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
        val nodeStreaming by nodeLLMRequestStreamingAndSendResults() // return List<Message.Response>

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

//        val lastRequest by node<List<Message.Response>, ResponseStreamList> { input ->
//            ResponseStreamList(input) //covert bean
//        }

        val lastRequest by node<List<Message.Response>, String> { input ->
            ResponseStreamList(input) //covert bean
            input.toString()
        }

        edge(nodeStart forwardTo mapStringToRequests)
        edge(mapStringToRequests forwardTo applyRequestToSession)
        edge(applyRequestToSession forwardTo nodeStreaming)
        edge(nodeStreaming forwardTo executeMultipleTools onMultipleToolCalls { true })
        edge(executeMultipleTools forwardTo mapToolCallsToRequests)
        edge(mapToolCallsToRequests forwardTo applyRequestToSession)
//        edge(
//            nodeStreaming forwardTo nodeFinish onCondition {
//                it.filterIsInstance<Message.Tool.Call>().isEmpty() //不再调用工具，就输出noteStreaming
//            }
//        )

        //增加对数据类型的转换
        edge(
            nodeStreaming forwardTo lastRequest onCondition { response ->
                //AI又推荐一个判断，有流式回复一段话再调工具的情景
                response.filterIsInstance<Message.Tool.Call>().isEmpty() //不再调用工具，就输出noteStreaming
            }
        )

        edge(
            lastRequest forwardTo nodeFinish
        )

    }
}