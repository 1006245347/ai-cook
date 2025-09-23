//package com.hwj.cook.agent
//
//import ai.koog.agents.core.agent.AIAgent
//import ai.koog.agents.core.agent.config.AIAgentConfig
//import ai.koog.agents.core.dsl.builder.forwardTo
//import ai.koog.agents.core.dsl.builder.strategy
//import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
//import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
//import ai.koog.agents.core.tools.ToolRegistry
//import ai.koog.prompt.message.Message
//import ai.koog.prompt.message.RequestMetaInfo
//import ai.koog.agents.core.environment.ReceivedToolResult
//import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreaming
//import ai.koog.agents.ext.tool.SayToUser
//import ai.koog.agents.features.eventHandler.feature.handleEvents
//import ai.koog.prompt.dsl.prompt
//import ai.koog.prompt.executor.clients.openai.OpenAIModels
//import com.benasher44.uuid.uuid4
//
//fun AgentStreamEx() {
//    val toolRegistry = ToolRegistry {
//        SayToUser
//    }
//
//    val agent = openAiAgent(toolRegistry) {
//        handleEvents {
//            onToolCall { context ->
//                println("\n🔧 Using ${context.tool.name} with ${context.toolArgs}... ")
//            }
////            onStreamFrame { context ->
////                (context.streamFrame as? StreamFrame.Append)?.let { frame ->
////                    print(frame.text)
////                }
////            }
////            onStreamError {
////                println("❌ Error: ${it.error}")
////            }
////            onAfterStream {
////                println("")
////            }
//        }
//    }
//}
//
//
//private fun openAiAgent(
//    toolRegistry: ToolRegistry,
//    installFeature: AIAgent.FeatureContext.() -> Unit
//) = AIAgent(
//    promptExecutor = createAiExecutor(""),
//    strategy = streamingWithToolsStrategy(),
//    agentConfig = agentStreamConfig(),
//    toolRegistry = toolRegistry, installFeatures = installFeature
//)
//
//fun agentStreamConfig() = AIAgentConfig(
//    prompt = prompt("chat${uuid4()}") { system { "" } },
//    model = OpenAIModels.Chat.GPT4o,
//    maxAgentIterations = 10
//)
//
//fun streamingWithToolsStrategy() = strategy("streaming_loop") {
//    val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
//    val nodeStreaming by nodeLLMRequestStreaming()
//
//    val mapStringToRequests by node<String, List<Message.Request>> { input ->
//        listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
//    }
//
//    val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input ->
//        llm.writeSession {
//            updatePrompt {
//                input.filterIsInstance<Message.User>()
//                    .forEach {
//                        user(it.content)
//                    }
//
//                tool {
//                    input.filterIsInstance<Message.Tool.Result>()
//                        .forEach {
//                            result(it)
//                        }
//                }
//            }
//            input
//        }
//    }
//
//    val mapToolCallsToRequests by node<List<ReceivedToolResult>, List<Message.Request>> { input ->
//        input.map { it.toMessage() }
//    }
//
////    edge(nodeStart forwardTo mapStringToRequests)
////    edge(mapStringToRequests forwardTo applyRequestToSession)
////    edge(applyRequestToSession forwardTo nodeStreaming)
////    edge(nodeStreaming forwardTo executeMultipleTools onMultipleToolCalls { true })
////    edge(executeMultipleTools forwardTo mapToolCallsToRequests)
////    edge(mapToolCallsToRequests forwardTo applyRequestToSession)
////    edge(
////        nodeStreaming forwardTo nodeFinish onCondition {
////            it.filterIsInstance<Message.Tool.Call>().isEmpty()
////        }
////    )
//
////    edge(nodeStart forwardTo nodeFinish)
//}
