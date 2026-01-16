package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.getCacheString
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.headers
import io.modelcontextprotocol.kotlin.sdk.client.mcpSse
import io.modelcontextprotocol.kotlin.sdk.client.mcpSseTransport
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ASearchTool : Tool<ASearchTool.Args, ASearchTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "bailian_web_search",
    description = "实时搜索互联网公开内容"
) {

    @Serializable
    data class Args(
        @property:LLMDescription("user query in the format of string") val query: String,
        @property:LLMDescription("number of search results") val count: Int
    )

    @Serializable
    data class Result(val result: String)

    override suspend fun execute(args: Args): Result {
        val key = getCacheString(DATA_MCP_KEY)
        val client = createKtorHttpClient(15000, {})


//        val mcpClient = client.mcpSse(urlString = null, requestBuilder = {
//            headers { append(HttpHeaders.Authorization, "Bearer $key") }
//            url {
//                protocol = URLProtocol.HTTPS
//                host = "dashscope.aliyuncs.com"
//                encodedPath = "/api/v1/mcps/WebSearch/sse"
//
//            }
//        })

        val result = StringBuilder()
        client.sse("https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse", request = {
            headers { append(HttpHeaders.Authorization, "Bearer $key") }
            url {
                protocol = URLProtocol.HTTPS
                host = "dashscope.aliyuncs.com"
                encodedPath = "/api/v1/mcps/WebSearch/sse"
            }

            contentType(ContentType.Application.Json)
            setBody(args)
        }) {
            incoming.collect { event ->
                println("Id: ${event.id}")
                println("Event: ${event.event}")
                println("Data: ${event.data}")
                result.append(event.data)
            }
        }

        return Result(result.toString())
    }
}