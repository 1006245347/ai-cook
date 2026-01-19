package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.sse.sseSession
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.headers
import io.ktor.server.util.url
import io.ktor.utils.io.readUTF8Line
import io.modelcontextprotocol.kotlin.sdk.client.mcpSse
import io.modelcontextprotocol.kotlin.sdk.client.mcpSseTransport
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @author by jason-何伟杰，2026/1/17
 * des:用百炼广场的sse接口构建mcp工具
 * koog的不能直接调用sse的工具接口，百炼调用的是标准mcp协议参数去搜索，不会返回koog需要的工具列表，二者没有关联
 */
object ASearchTool : Tool<ASearchTool.Args, ASearchTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "bailian_web_search",
    description = "实时搜索互联网公开内容,包括查询今天网络日期"
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

        var messagePath: String? = null
        val result = StringBuilder()
        //ktor 的 sse是get
        client.sse("https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/sse", request = {
            headers { append(HttpHeaders.Authorization, "Bearer $key") }
            url {
                protocol = URLProtocol.HTTPS
                host = "dashscope.aliyuncs.com"
                encodedPath = "/api/v1/mcps/WebSearch/sse"

//                parameters.append("query", args.query)
//                parameters.append("count", args.count.toString())
            }
            contentType(ContentType.Application.Json)
            setBody(C1("今天珠海天气？", 1))
        }) {
            incoming.collect { event ->
                println("Id: ${event.id}")
                println("Event: ${event.event}")
                println("Data: ${event.data}")
//                result.append(event.data) //只是返回下次请求的sse地址 如  /api/v1/mcps/WebSearch/message?sessionId=e94ac0ab41ee4a43bfc2c70302bc9291
                messagePath = event.data
            }
        }

        printD("msgPath>$messagePath")
        if (messagePath == null) {

        } else {
//            val channel = client.get("https://dashscope.aliyuncs.com$messagePath") {
//                headers {
//                    append(HttpHeaders.Authorization, "Bearer $key")
////                    append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
//                    append(HttpHeaders.Accept,"*/*")
//                }
//            }.bodyAsChannel()
//            while (!channel.isClosedForRead) {
//                val line = channel.readUTF8Line()
//                printD("Line>$line")
////                if (!line.isNullOrBlank()) {
//                    result.append(line)
////                }
//            }

            val response = client.get("https://dashscope.aliyuncs.com$messagePath") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $key")
//                    append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
                    append(HttpHeaders.Accept,"*/*")
                }
            }.bodyAsText()

            result.append(response)

//            client.sse("https://dashscope.aliyuncs.com$messagePath", request = {
//                headers {
//                    append(HttpHeaders.Authorization, "Bearer $key")
//                    append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
//                }
//            }) {
//                incoming.collect { event ->
////            printD("Id: ${event.id}")
//                    printD("Event: ${event.event}")
//                    printD("Data: ${event.data}")
//                    result.append(event.data)
//                }
//            }

            printD("ss>$result")
        }


        printD("web_tool>${result}")
        return Result(result.toString())
    }
}