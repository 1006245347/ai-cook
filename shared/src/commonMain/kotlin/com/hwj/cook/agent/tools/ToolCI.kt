package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.ToolResultUtils
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.getDeviceInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2025/10/13
 * des:工具集合
 */
object ToolCI {

    object DiagnosticTool : Tool<DiagnosticTool.Args, DiagnosticTool.Result>() {
        override val argsSerializer: KSerializer<Args>
            get() = Args.serializer()
        override val resultSerializer: KSerializer<Result>
            get() = ToolResultUtils.toTextSerializer<Result>()
        override val description: String
            get() = "获取设备信息的工具"

        override suspend fun execute(args: Args): Result {
            return Result("cpu-babalala")
        }

        @Serializable
        data class Args(@property: LLMDescription("设备信息") val device: String)

        @Serializable
        data class Result(val cpu: String) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "当前设备cpu: $cpu"
            }
        }

        fun printDeviceInfo() {
            val info = getDeviceInfo()
            println("Platform: ${info.platform}")
            println("CPU: ${info.cpuCores} cores (${info.cpuArch})")
            println("Memory: ${info.totalMemoryMB} MB")
            println("Brand: ${info.brand}, Model: ${info.model}")
            println("OS: ${info.osVersion}")
        }
    }

    object UserInfoTool : Tool<UserInfoTool.Args, UserInfoTool.Result>() {
        override val argsSerializer: KSerializer<Args>
            get() = Args.serializer()
        override val resultSerializer: KSerializer<Result>
            get() = ToolResultUtils.Companion.toTextSerializer<Result>()
        override val description: String
            get() = "获取用户信息的工具"

        override suspend fun execute(args: Args): Result {
            return Result("x7")
        }

        @Serializable
        data class Args(@property:LLMDescription("user 's name") val name: String)

        @Serializable
        data class Result(val name: String) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "user name is $name"
            }
        }
    }
}