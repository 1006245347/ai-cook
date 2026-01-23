package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.benasher44.uuid.uuid4
import com.hwj.cook.agent.createAiExecutor
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString


/**
 * @author by jason-何伟杰，2025/10/16
 * des:demo是让agent调用工具把开关打开。。
 */
//suspend fun AgentStreamEx() {
//    val toolRegistry = ToolRegistry {
//        SayToUser
//    }
//    val key = getCacheString(DATA_APP_TOKEN)!!
//    val agent = openAiAgent(key, toolRegistry) {
//        handleEvents {
//            onToolCallStarting { context ->
//                println("\n🔧 Using ${context.tool.name} with ${context.toolArgs}... ")
//            }
//            onLLMStreamingFrameReceived { context ->
//                (context.streamFrame as? StreamFrame.Append)?.let { frame ->
//                    print(frame.text)
//                }
//            }
//            onLLMStreamingFailed {
//                println("❌ Error: ${it.error}")
//            }
//            onLLMStreamingCompleted {
//                println("onLLMStreamingCompleted>")
//
//            }
//        }
//    }
//
//    agent.run("hello")
//}

//只要这段代码不注释，必报错！是输入输出类型导致生成代码报错？
//private fun openAiAgent(
//    apiKey: String,
//    toolRegistry: ToolRegistry,
//    installFeature: FeatureContext.() -> Unit = {}
//) =
//    AIAgent(
//        promptExecutor = createAiExecutor(apiKey),
//        strategy = streamingWithToolsStrategy(),
//        llmModel = OpenAIModels.Chat.GPT4o,
//        systemPrompt = "You're responsible for running a Switch and perform operations on it by request",
//        temperature = 0.0,
//        toolRegistry = toolRegistry,
//        installFeatures = installFeature
//    )

//private fun openAiAgent(
//    apiKey: String,
//    toolRegistry: ToolRegistry,
//    installFeature: FeatureContext.() -> Unit = {}
//) = AIAgent(
//    promptExecutor = createAiExecutor(apiKey),
//    agentConfig = agentStreamConfig(),
//    strategy = streamingWithToolsStrategy(),
//
//    toolRegistry = toolRegistry, installFeatures = installFeature
//)

fun agentStreamConfig() = AIAgentConfig(
    prompt = prompt("chat${uuid4()}") { system("You're responsible for running a Switch and perform operations on it by request") },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 10
)

fun streamingWithToolsStrategy1() = strategy("streaming_loop") {
    val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
    val nodeStreaming by nodeLLMRequestStreamingAndSendResults()

    val mapStringToRequests by node<String, List<Message.Request>> { input ->
        listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
    }

    val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input ->
        llm.writeSession {
            updatePrompt {
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
