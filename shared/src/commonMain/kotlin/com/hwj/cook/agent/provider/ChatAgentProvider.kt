package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.isFinished
import ai.koog.agents.core.agent.isRunning
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.agent.createAiExecutor
import com.hwj.cook.agent.tools.McpToolCI
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD

/**
 * @author by jason-何伟杰，2025/10/11
 * des:问答对话只需要会话消息和大模型接口即可，不需要完整的智能体能力，
 * 这里写个简单的问答智能体用来实现简单的问答对话
 */
class ChatAgentProvider(
    override var title: String = "Chat",
    override val description: String = "A conversational agent that supports long-term memory, with clear and concise responses."
) : AgentProvider<String, String> {

    private var agentInstance: AIAgent<String, String>? = null
//    private val msgHistory = mutableListOf<Pair<String, String>>() //用户问题-》智能体回答

    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        val mcpKey = getCacheString(DATA_MCP_KEY)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }

        //智能体默认非流式回复？？？
//        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))
        val remoteAiExecutor = createAiExecutor(apiKey)

//        DeepSeekLLMClient
//        OpenAILLMClient

//        MultiLLMPromptExecutor () //混合客户端
//        DefaultMultiLLMPromptExecutor()
        val toolRegistry = ToolRegistry {
//            tool(ExitTool)
//            tool(ASearchTool) //endpoint是给后端接的。。我用不了
        }
//            .plus(McpToolCI.searchSSE2())
//            .plus(McpToolCI.searchSSE3())
//            .plus(McpToolCI.searchSSE4()) //暂时没法实现多平台的mcp，先放放
//            .plus(McpToolCI.webParserSSE(mcpKey))
        toolRegistry.tools.forEach { printD(it.name + "：" + it.descriptor, "Atool>") }
        agentInstance = AIAgent.invoke(
            promptExecutor = remoteAiExecutor,
            toolRegistry = toolRegistry,
//            strategy = ,
            agentConfig = AIAgentConfig(
                prompt = prompt(
                    id = "chat",
                    params = LLMParams(temperature = 0.8, numberOfChoices = 1, maxTokens = 1500)
                ) {
                    system("I'm an assistant who provides simple and clear answers to users.")
//                    system ("""
//You are an assistant that must use tools to answer any question
//that requires real-time or factual information such as dates, news, or web content.
//If a tool is available, you should call it instead of answering directly.
//""")
                },
                model = buildQwen3LLM("Qwen/Qwen2.5-7B-Instruct"),
                maxAgentIterations = 50 //太少反而会导致无法结束智能体
            ),
        )

        return agentInstance!!
    }

    //函数式设计策略
//    private fun exportMsgStrategy(): AIAgentFunctionalStrategy<String, Unit> {
//        return functionalStrategy {
//            var userResponse = it
//            while (userResponse != "/bye") {
//                val responses = requestLLM(userResponse)
//                msgHistory += userResponse to responses.content
//                userResponse = readln() //专用控制台的输入
//            }
//        }
//    }

    suspend fun isRunning(): Boolean {
        return agentInstance!!.isRunning()
    }

    suspend fun isFinished(): Boolean {
        return agentInstance!!.isFinished()
    }
}