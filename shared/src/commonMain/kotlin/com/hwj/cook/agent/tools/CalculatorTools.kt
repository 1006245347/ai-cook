package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2025/12/4
 * des: 多平台用Tool ,jvm平台用ToolSet
 */
sealed class CalculatorTools(name: String, description: String) :
    Tool<CalculatorTools.Args, CalculatorTools.Result>(
        argsSerializer = Args.serializer(),
        resultSerializer = Result.serializer(),
        name = name,
        description = description
    ) {

    @Serializable
    data class Args(
        @property:LLMDescription("First number")
        val a: Float,
        @property:LLMDescription("Second number")
        val b: Float
    )

    @Serializable
    class Result(val result: Float)


    /**
     * 2. Implement the tool (tools).
     */

    object PlusTool : CalculatorTools(
        name = "plus",
        description = "Adds a and b",
    ) {
        override suspend fun execute(args: Args): Result {
            return Result(args.a + args.b)
        }
    }

    object MinusTool : CalculatorTools(
        name = "minus",
        description = "Subtracts b from a",
    ) {
        override suspend fun execute(args: Args): Result {
            return Result(args.a - args.b)
        }
    }

    object DivideTool : CalculatorTools(
        name = "divide",
        description = "Divides a and b",
    ) {
        override suspend fun execute(args: Args): Result {
            return Result(args.a / args.b)
        }
    }

    object MultiplyTool : CalculatorTools(
        name = "multiply",
        description = "Multiplies a and b",
    ) {
        override suspend fun execute(args: Args): Result {
            return Result(args.a * args.b)
        }
    }
}