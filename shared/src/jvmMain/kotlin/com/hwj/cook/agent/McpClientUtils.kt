package com.hwj.cook.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
/**
 * @author by jason-何伟杰，2025/8/26
 * des:需要安装本地npx命令   mcp大部分都只适合nodejs/jvm
 */
object McpClientUtils {

    suspend fun t1() {
//        McpToolRegistryProvider.fromTransport()

    }

    //不是所有环境有nodejs,最好引入modelcontextprotocol/kotlin-sdk ,还可以引入ktor-server/client
    suspend fun searchMcpClientSSE(input: String) {
        val transport =
            McpToolRegistryProvider.defaultSseTransport("https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse")
        val toolRegistry = McpToolRegistryProvider.fromTransport(transport)

        toolRegistry.tools.forEach {
            printD(it.name+"："+it.descriptor)
        }



    }



    //注意mcp的进程要销毁
    suspend fun searchMcpClientStudio(input: String) {
        val AUTH_HEADER = "Bearer sk-a6429dbcef8e49fc88d08c039cd3c2c6"
        createMcpClientStudio(
            input,
            "D:\\ideawork\\node\\npx.cmd",
            "mcp-remote",
            "https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse",
            "--header",
            "Authorization:${AUTH_HEADER}",
            onAssistantMessage = { "" })
    }

    @Suppress("DuplicatedCode")
    suspend fun createMcpClientStudio(
        input: String,
        vararg args: String,
        onAssistantMessage: suspend (String) -> String
    ) {
        val process = ProcessBuilder(*args).start()//可变参数展开
        val toolRegistry = McpToolRegistryProvider.fromTransport(
            transport = McpToolRegistryProvider.defaultStdioTransport(process)
        )
        toolRegistry.tools.forEach { //上面就完成所有tool的加入,但是还未注明tool在提问中进行使用
            println(it.name)
            println(it.descriptor)
        }
        toolRegistry.add(ExitTool)
        //如果我要加多个mcp工具，启动多个服务？多个进程
//        toolRegistry.plus()

        val token = getCacheString(DATA_APP_TOKEN)

        val executor =
            SingleLLMPromptExecutor(OpenAiRemoteLLMClient(token!!))

        val strategy = strategy("mcp-search") {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)//一个执行多个工具调用的节点。这些调用可以选择并行执行。
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults() //一个将多个工具结果添加到提示中并获取多个 LLM 响应的节点
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            edge(nodeStart forwardTo nodeRequestLLM)

            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                        onMultipleToolCalls { true })

            edge(
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true })

            edge(nodeAssistantMessage forwardTo nodeRequestLLM)

            edge(
                nodeExecuteToolMultiple forwardTo nodeFinish
                        onCondition { it.singleOrNull()?.tool == ExitTool.name }
                        transformed { it.single().result!!.toString() })

            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                        onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                        onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                        onMultipleToolCalls { true }
            )

            edge(
                nodeSendToolResultMultiple forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )
        }

        val agent = AIAgent(
            promptExecutor = executor, llmModel = OpenAIModels.Chat.GPT4o,
            systemPrompt = "你是个强大的智能体助手，可使用配置的Mcp工具搜索服务信息。",
            strategy = strategy,// singleRunStrategy(),
            toolRegistry = toolRegistry
        )
        {
            handleEvents {
                onToolCallStarting { ctx ->
                    printD("t>${ctx.tool.name}, args ${ctx.toolArgs} ,${ctx.toolCallId}")
//                    onToolCallEvent("Tool ${ctx.tool.name}, args ${ctx.toolArgs}")
                }

                onAgentExecutionFailed { ctx ->
                    printD("e>${ctx.throwable.message}")
//                    onErrorEvent("${ctx.throwable.message}")
                }

                onAgentCompleted { ctx ->
                    printD("f>${ctx.result}")
                    // Skip finish event handling
                }
            }
        }
        var result = agent.run("$input")
        printD("r>$result")
    }
}