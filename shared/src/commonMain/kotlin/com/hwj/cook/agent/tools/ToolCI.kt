package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.KmpToolSet
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.getDeviceInfo
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.models.DeviceInfoCell
import com.hwj.cook.models.SuggestCookSwitch
import io.ktor.client.plugins.sse.sse
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
import io.ktor.utils.io.readUTF8Line
import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2025/10/13
 * des:工具集合示例
 *
 * sdk自带的tool,  agents/agents-ext/src/commonMain/kotlin/ai/koog/agents/ext/tool
 *
 * 描述arg参数，在execute接收参数后执行逻辑构建json结果，整个工具的描述也是对json的定义
 *
 * demo有个SimpleTool的设计、还有个ToolSet(靠jvm反射实现注解等，ios不适用)
 */

object DiagnosticTool : Tool<DiagnosticTool.Args, DiagnosticTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "DeviceTool", description = "get user device information"
) {
    override suspend fun execute(args: Args): Result {
        val devInfo = printDeviceInfo()
        return Result(JsonApi.encodeToString(devInfo))
    }

    @Serializable
    data class Args(@property:LLMDescription("device information") val device: String)

    @Serializable
    data class Result(val devInfo: String)

    fun printDeviceInfo(): DeviceInfoCell {
        val info = getDeviceInfo()
        println("Platform: ${info.platform}")
        println("CPU: ${info.cpuCores} cores (${info.cpuArch})")
        println("Memory: ${info.totalMemoryMB} MB")
        println("Brand: ${info.brand}, Model: ${info.model}")
        println("OS: ${info.osVersion}")
        return info
    }
}

object UserInfoTool : Tool<UserInfoTool.Args, UserInfoTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "UserInfoTool", description = "get user information"
) {
    override suspend fun execute(args: Args): Result {
        return Result(args.name)
    }

    @Serializable
    data class Args(@property:LLMDescription("user 's information") val name: String)

    @Serializable
    data class Result(val name: String)
}

//need more time ,dep real rag.

//??使用TooSet设计一个开关，agent去操作开关。不能直接用ToolSet只在jvm
class SuggestSwitchTools(val switch: SuggestCookSwitch) : KmpToolSet {

    fun change() {

    }
}

suspend fun testMcp11() {
    val key = getCacheString(DATA_MCP_KEY)
    val client = createKtorHttpClient(15000, {})
    val result = StringBuilder()
    val response = client.post {
        headers {
//            append(HttpHeaders.Authorization, "Bearer $key")
            append(HttpHeaders.Accept, "text/event-stream")
            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        }
        contentType(ContentType.Application.Json)
        url {
            protocol = URLProtocol.HTTPS
            host = "dashscope.aliyuncs.com"
            encodedPath = "/api/v1/mcps/WebSearch/sse"
        }


        setBody(JsonApi.encodeToString(C1("今天珠海天气？", 1)))

    }



    result.append(response.bodyAsText())

    printD("web_tool>${result}")
}

suspend fun testMcp2() {
    val key = getCacheString(DATA_MCP_KEY)
    val client = createKtorHttpClient(15000, {})

    val result = StringBuilder()
    var messagePath: String? = null
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
//            result.append(event.data)
            messagePath = event.data
        }
    }
    //msgPath>  /api/v1/mcps/WebSearch/message?sessionId=e94ac0ab41ee4a43bfc2c70302bc9291
//    val channel = client.get("https://dashscope.aliyuncs.com$messagePath") {
//        headers {
//            append(HttpHeaders.Authorization, "Bearer $key")
//            append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
//        }
//    }.bodyAsChannel()
//    while (!channel.isClosedForRead) {
//        val line = channel.readUTF8Line()
//        printD("Line>$line")
//        result.append(line)
//    }

    val response = client.get("https://dashscope.aliyuncs.com$messagePath") {
        headers {
            append(HttpHeaders.Authorization, "Bearer $key")
//                    append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
            append(HttpHeaders.Accept, "*/*")
        }
    }.bodyAsText()

    result.append(response)

//    client.sse("https://dashscope.aliyuncs.com$messagePath", request = {
//        headers {
//            append(HttpHeaders.Authorization, "Bearer $key")
//            append(HttpHeaders.Accept, ContentType.Text.EventStream.toString())
//        }
//    }) {
//        incoming.collect { event ->
////            printD("Id: ${event.id}")
//            printD("Event: ${event.event}")
//            printD("Data: ${event.data}")
//            result.append(event.data)
//        }
//    }

    printD("ss>$result")

}

@Serializable
data class C1(val query: String, val count: Int)