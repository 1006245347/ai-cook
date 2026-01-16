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
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolDescriptorParser
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printList
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.http.headers
import io.ktor.server.util.url
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.mcpSse
import io.modelcontextprotocol.kotlin.sdk.client.mcpSseTransport
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttpTransport
import io.modelcontextprotocol.kotlin.sdk.client.mcpWebSocketTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * @author by jason-何伟杰，2025/8/26
 * des:需要安装本地npx命令   mcp大部分都只适合nodejs/jvm
 */
object McpClientUtils {

    //无语了，百炼广场的接口协议上返回的数据不是tools，所以不会直接显示
    suspend fun testMcp() {
        val url = "https://dashscope.aliyuncs.com/api/v1/mcps/zhipu-websearch/sse"
        val mcpKey = getCacheString(DATA_MCP_KEY)
        val tools = withTimeout(15.seconds) {
            val transport = SseClientTransport(client = createKtorHttpClient(15000, builder = {
            }), null, requestBuilder = {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $mcpKey")
                }
                url { //https://dashscope.aliyuncs.com/api/v1/mcps/zhipu-websearch/sse
                    protocol = URLProtocol.HTTPS
                    host = "dashscope.aliyuncs.com"
                    encodedPath = "/api/v1/mcps/zhipu-websearch/sse"
                }
            })

            printD("tr?$transport")
//            McpToolRegistryProvider.fromTransport(
//                transport = transport,
//                name = "test1", version = "0.1.0",
//            )
            val mcpClient = Client(Implementation(name = "hwj1", version = "0.1.0"))

            mcpClient.connect(transport)
            val sdkTools = mcpClient.listTools().tools


            printList(sdkTools, "s")
        }
    }

    suspend fun tryClient() {
        val mKey = getCacheString(DATA_MCP_KEY).also { printD("key= $it") }

        val client: HttpClient = createKtorHttpClient(15000, builder = {
//            append(HttpHeaders.Authorization, "Bearer $mcpKey")
        })


        val transport = client.mcpSseTransport(urlString = null) {
            headers { append(HttpHeaders.Authorization, "Bearer $mKey") }
            url { //https://dashscope.aliyuncs.com/api/v1/mcps/zhipu-websearch/sse
                protocol = URLProtocol.HTTPS
                host = "dashscope.aliyuncs.com"
                encodedPath = "/api/v1/mcps/zhipu-websearch/sse"

            }
        }

        val toolRegistry =
            McpToolRegistryProvider.fromTransport(transport, name = "bailian_web_search")
//        transport.close() //没什么用呀

        toolRegistry.tools.forEach { printD(it.name + "：" + it.descriptor, "tool>") }

    }

    suspend fun tryClient2() {
        val mKey = getCacheString(DATA_MCP_KEY).also { printD("key= $it") }

        val client: HttpClient = createKtorHttpClient(15000, builder = {
//            append(HttpHeaders.Authorization, "Bearer $mcpKey")
        })


        val transport: SseClientTransport = client.mcpSseTransport(urlString = null) {
            headers { append(HttpHeaders.Authorization, "Bearer $mKey") }
            url { //https://dashscope.aliyuncs.com/api/v1/mcps/zhipu-websearch/sse
                protocol = URLProtocol.HTTPS
                host = "dashscope.aliyuncs.com"
                encodedPath = "/api/v1/mcps/zhipu-websearch/sse"

            }
        }

        val toolRegistry = McpToolRegistryProvider.fromTransport(transport, name = "")
        toolRegistry.tools.forEach { printD(it.name + "：" + it.descriptor, "tool>") }
    }

    suspend fun tryClient3() {
        val mKey = getCacheString(DATA_MCP_KEY).also { printD("key= $it") }

        val client: HttpClient = createKtorHttpClient(15000, builder = {
//            append(HttpHeaders.Authorization, "Bearer $mcpKey")
        })


        val mcpClient: Client = client.mcpSse {
            headers { append(HttpHeaders.Authorization, "Bearer $mKey") }
            url { //https://dashscope.aliyuncs.com/api/v1/mcps/zhipu-websearch/sse
                protocol = URLProtocol.HTTPS
                host = "dashscope.aliyuncs.com"
                encodedPath = "/api/v1/mcps/zhipu-websearch/sse"

            }
        }

//        mcpClient.close()

        printD("tr>${mcpClient.transport}")
//        mcpClient.transport?.start()

        McpToolRegistryProvider.fromTransport(mcpClient.transport!!, name = "bailian_web_search")
//        val toolRegistry = McpToolRegistryProvider.fromClient(mcpClient)
//        toolRegistry.tools.forEach { printD(it.name + "：" + it.descriptor, "tool>") }
    }

    //不是所有环境有nodejs,最好引入modelcontextprotocol/kotlin-sdk ,还可以引入ktor-server/client
    //用百炼广场的mcp工具，mcp服务端在云服务器，我这实现客户端就行，不走本地mcp就不用nodejs
    suspend fun searchMcpClientSSE(input: String, mcpKey: String): ToolRegistry {
        val transport =
            McpToolRegistryProvider.defaultSseTransport(
                "https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse",
                baseClient = createKtorHttpClient(15000, builder = {
                    append(HttpHeaders.Authorization, "Bearer $mcpKey")
                })
            )
        val toolRegistry =
            McpToolRegistryProvider.fromTransport(transport, name = "bailian_web_search")

        printD("tr>$toolRegistry")
        toolRegistry.tools.forEach {
            printD(it.name + "：" + it.descriptor)
        }
        return toolRegistry
    }

    //这里有个大坑，config{}会覆盖我们的，导致apiKey不引入
//    public fun defaultSseTransport(url: String, baseClient: HttpClient = HttpClient()): SseClientTransport {
//        // Setup SSE transport using the HTTP client
//        return SseClientTransport(
//            client = baseClient.config {
//                install(SSE)
//            },
//            urlString = url,
//        )
//    }


    //注意mcp的进程要销毁
    suspend fun searchMcpClientStudio(input: String, mcpKey: String) {
        val AUTH_HEADER = "Bearer $mcpKey"
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
                    printD("t>${ctx.toolName}, args ${ctx.toolArgs} ,${ctx.toolCallId}")
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