package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.mcp.McpToolRegistryProvider
import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.global.printD
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.shared.Transport

object McpToolCI {


    //不是所有环境有nodejs,最好引入modelcontextprotocol/kotlin-sdk ,还可以引入ktor-server/client
    //用百炼广场的mcp工具，mcp服务端在云服务器，我这实现客户端就行，不走本地mcp就不用nodejs
    //sse这种方式全平台可用，stdio带process只能jvm
    suspend fun searchSSE(mcpKey: String): ToolRegistry {
        //token 在executor再给？ ,不对，百炼一个token,chat又一个
//        header("Authorization", "Bearer $apiKey")
        val transport =
            McpToolRegistryProvider.defaultSseTransport(
                "https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse",
                baseClient = createKtorHttpClient(15000, builder = {
                    headers {
                        append("cook", "ai")
                        append(HttpHeaders.Authorization, "Bearer $mcpKey")
                    }
                })
            )
        val toolRegistry = McpToolRegistryProvider.fromTransport(transport, name = "search web")

        toolRegistry.tools.forEach {
            printD(it.name + "：" + it.descriptor)
        }
        return toolRegistry
    }

    suspend fun webParserSSE(mcpKey: String): ToolRegistry {
        //1.构建mcpClient,可以用默认的

        //2.mcp工具的sse接口
        val transport =
            McpToolRegistryProvider.defaultSseTransport(
                "https://dashscope.aliyuncs.com/api/v1/mcps/WebParser/sse",
                baseClient = createKtorHttpClient(15000, builder = {
                    headers { append(HttpHeaders.Authorization, "Bearer $mcpKey") }
                })
            )

        val toolRegistry = McpToolRegistryProvider.fromTransport(transport, name = "web parser")
        return toolRegistry
    }

    //mcp sdk构建 走stdio多不支持手机
    suspend fun mcpClient(name: String, transport: Transport): ToolRegistry {
        val mcpClient = Client(
            clientInfo = io.modelcontextprotocol.kotlin.sdk.types.Implementation(
                name = name,
                version = "1.0.0"
            )
        )
//        mcpClient.connect(transport) //sdk有这个
        return McpToolRegistryProvider.fromClient(
            mcpClient
        )
    }

}