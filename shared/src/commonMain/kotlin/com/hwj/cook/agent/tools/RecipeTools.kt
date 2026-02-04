package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * @author by jason-何伟杰，2025/10/10
 * des:智能体工具的设计
 */
object RecipeTools {

    //从本地知识库找内容
    object SearchRecipeTool : Tool<SearchRecipeTool.Args, SearchRecipeTool.Result>(
        argsSerializer = Args.serializer(),
        resultSerializer = Result.serializer(),
        name = "searchRecipe",
        description = "搜索合适的菜谱"
    ) {

        @OptIn(ExperimentalTime::class)
        override suspend fun execute(args: Args): Result {

//            return Result(Clock.System.now().toString(), "杀鱼，放姜去鱼鳞")
            return Result(Clock.System.now().toString(), "关键内容的菜谱查找：" + args.needInput)
        }

        //定义工具的参数
        @Serializable
        data class Args(@property:LLMDescription("从本地知识库获取菜谱菜式") val needInput: String = "查找关键字")

        //定义给LLm的参数结构   //曾经的写法，又改了
//        @Serializable
//        data class Result(val datetime: String, val recipe: String) :
//            ToolResult.TextSerializable() {
//            override fun textForLLM(): String {
//                return "当前日期:$datetime,所查找的菜谱菜式内容:$recipe"
//            }
//        }
        @Serializable
        data class Result(val datetime: String, val recipe: String)
    }

    //获取用户的口味偏好
    object UserFlavorTool : Tool<UserFlavorTool.Args, UserFlavorTool.Result>(
        argsSerializer = Args.serializer(),
        resultSerializer = Result.serializer(),
        name = "userFlavor",
        description = "获取口味偏好的工具"
    ) {
        override suspend fun execute(args: Args): Result {
            return Result(args.userFlavor) //这里要加？？工具逻辑
        }

        @Serializable
        data class Args(
            @property:LLMDescription("口味偏好")
            val userFlavor: String
        )

        @Serializable
        data class Result(val flavor: String)
    }
}