package com.hwj.cook.agent.provider

import com.hwj.cook.agent.tools.SwitchTools
import com.hwj.cook.models.SuggestCookSwitch

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString

suspend fun testConsoleAgent1() {
    val switch = SuggestCookSwitch()

    val toolRegistry = ToolRegistry {
        tools(SwitchTools(switch).asTools())
    }
////    val apiKey = getCacheString(DATA_APP_TOKEN)!! //java测试时不能用，setting没搞
    val apiKey = "ssss"
    SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey = apiKey)).use { executor ->
        //agent的类型 GraphAIAgent<String, List<Message.Response>>
//        val agent= anthropicAgent(toolRegistry,executor)

//        executor.executeStreaming() //见鬼？
        val agent = chatAiAgent(toolRegistry, executor) {

            handleEvents {
                onToolCallStarting { context ->
                    println("\n🔧 Using ${context.toolName} with ${context.toolArgs}... ")
                }
                onLLMStreamingFrameReceived { context ->
                    (context.streamFrame as? StreamFrame.Append)?.let { frame ->
                        print("rev>${frame.text}")
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

        println("Streaming chat agent started\nUse /quit to quit\nEnter your message(hwj):")
        var input = ""
        while (input != "/quit") {
            input = readln()  //读一次用户输入

            // Example message: 这个
            // Tell me if the switch if on or off. Elaborate on how you will determine that. After that, if it was off, turn it on. Be very verbose in all the steps

            agent.run(input)

            println()
            println("%%%%%%%%%%%%%%%Enter your message:")
        }
    }
}

fun unStreamAgent(
    toolRegistry: ToolRegistry,
    executor: PromptExecutor,
    installFeatures: FeatureContext.() -> Unit = {}
) = AIAgent.invoke(
    promptExecutor = executor,
    strategy = streamingWithToolsStrategy(),
    agentConfig = AIAgentConfig(//streaming = false  executor??
        prompt = prompt(
            id = "prompt",
            params = LLMParams(temperature = 0.0)
        ) {
            system("You're responsible for running a Switch and perform operations on it by request")
        }, model = buildQwen3LLM(), maxAgentIterations = 50
    ),
    toolRegistry = toolRegistry,
    installFeatures = installFeatures
)


fun chatAiAgent(
    toolRegistry: ToolRegistry,
    executor: PromptExecutor,
    installFeatures: FeatureContext.() -> Unit = {}
) = AIAgent(
    promptExecutor = executor,
    strategy = streamingWithToolsStrategy(),
    llmModel = buildQwen3LLM("Qwen/Qwen2.5-7B-Instruct"),
    systemPrompt = "You're responsible for running a Switch and perform operations on it by request",
    temperature = 0.0,
    toolRegistry = toolRegistry,
    installFeatures = installFeatures
)

@Suppress("unused")
private fun anthropicAgent(
    toolRegistry: ToolRegistry,
    executor: PromptExecutor,
    installFeatures: FeatureContext.() -> Unit = {}
) = AIAgent(
    promptExecutor = executor,
    strategy = streamingWithToolsStrategy(),
    llmModel = AnthropicModels.Sonnet_3_7,
    systemPrompt = "You're responsible for running a Switch and perform operations on it by request",
    temperature = 0.0,
    toolRegistry = toolRegistry,
    installFeatures = installFeatures
)

fun streamingWithToolsStrategy() = strategy("streaming_loop") {
    val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
    val nodeStreaming by nodeLLMRequestStreamingAndSendResults()

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
            it.filterIsInstance<Message.Tool.Call>().isEmpty()
        }
    )
}