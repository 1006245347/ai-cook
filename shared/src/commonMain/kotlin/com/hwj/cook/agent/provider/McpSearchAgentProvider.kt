package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.isFinished
import ai.koog.agents.core.agent.isRunning
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.message.Message
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.createMemoryProvider
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString

class McpSearchAgentProvider(
    override var title: String = "search from internet",
    override val description: String = "A agent that support search from internet with mcp tool."
) : AgentProvider<String, String> {

    private var agentInstance: AIAgent<String, String>? = null


    override suspend fun provideAgent(
        prompt: Prompt,
        onToolCallEvent: suspend (Message.Tool.Call) -> Unit,
        onToolResultEvent: suspend (Message.Tool.Result) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        val agentConfig =
            AIAgentConfig(prompt = prompt, model = OpenAIModels.Chat.GPT4o, maxAgentIterations = 50)

        val strategy = strategy<String, String>("mcp_search") {
            // 发送LLM请求（可生成多个工具调用或回复） LLM分析用户问题，决定是否调用工具
            val nodeRequestLLM by nodeLLMRequestMultiple()

//            输出LLM生成的消息到UI 回调 onAssistantMessage
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }

//            并行执行多个工具调用 比如计算加减乘除
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)

// 把工具执行结果反馈给LLM, LLM可能会再次推理或继续计算
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()

//            压缩LLM上下文 ,当消息过多时减少历史负担
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            //开始 LLM请求
            edge(nodeStart forwardTo nodeRequestLLM)

//            当LLM输出包含多个tool call时，进入工具执行节点。
            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                        onMultipleToolCalls { true }
            )

//            如果LLM输出的是文本消息（assistant回复），就走到消息节点。
            edge(
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            //再决策or结束  消息回路  当用户继续输入新内容，重新触发LLM处理。
            edge(nodeAssistantMessage forwardTo nodeRequestLLM)

            // Finish condition - if exit tool is called, go to nodeFinish with tool call result.
//            调用了退出工具（ExitTool） → 结束
            edge(
                nodeExecuteToolMultiple forwardTo nodeFinish
                        onCondition { it.singleOrNull()?.tool == ExitTool.name }
                        transformed { it.single().result!!.toString() }
            )

//            上下文太大 → 压缩再反馈
            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                        onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

//            上下文正常 → 直接反馈工具结果给LLM
            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                        onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            //工具结果反馈后
            //再次决策回路 ，再次生成新的工具调用(形成循环）
            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                        onMultipleToolCalls { true }
            )

            //输出文字结果给用户
            edge(
                nodeSendToolResultMultiple forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )
        }

        val userMemoryAgentProvider = createMemoryProvider()
        val agent = AIAgent.invoke(
            promptExecutor = remoteAiExecutor, strategy = strategy, agentConfig = agentConfig
        ) {
            install(AgentMemory.Feature) {
                this.memoryProvider = userMemoryAgentProvider
                this.productName = DATA_APPLICATION_NAME //设置产品名，为了范围对应
            }
        }
        return agent
    }

    suspend fun isRunning(): Boolean {
        return agentInstance!!.isRunning()
    }

    suspend fun isFinished(): Boolean {
        return agentInstance!!.isFinished()
    }
}