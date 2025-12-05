package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.KmpToolSet
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.getDeviceInfo
import com.hwj.cook.models.DeviceInfoCell
import com.hwj.cook.models.SuggestCookSwitch
import kotlinx.serialization.KSerializer
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

object DiagnosticTool : Tool<DiagnosticTool.Args, DiagnosticTool.Result>() {
    override val argsSerializer: KSerializer<Args>
        get() = Args.serializer()
    override val resultSerializer: KSerializer<Result>
        get() = Result.serializer()
    override val description: String
        get() = "获取设备信息的工具"

    override suspend fun execute(args: Args): Result {
        val devInfo = printDeviceInfo()
        return Result(JsonApi.encodeToString(devInfo))
    }

    @Serializable
    data class Args(@property: LLMDescription("设备信息") val device: String)

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

object UserInfoTool : Tool<UserInfoTool.Args, UserInfoTool.Result>() {
    override val argsSerializer: KSerializer<Args>
        get() = Args.serializer()
    override val resultSerializer: KSerializer<Result>
        get() = Result.serializer()
    override val description: String
        get() = "获取用户姓名的工具"

    override suspend fun execute(args: Args): Result {
        return Result(args.name)
    }

    @Serializable
    data class Args(@property:LLMDescription("user 's name") val name: String)

    @Serializable
    data class Result(val name: String)
}

//使用TooSet设计一个开关，agent去操作开关
class SuggestSwitchTools(val switch: SuggestCookSwitch): KmpToolSet{

    fun change(){

    }
}