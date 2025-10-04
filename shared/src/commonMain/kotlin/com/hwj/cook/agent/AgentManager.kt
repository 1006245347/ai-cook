package com.hwj.cook.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.AIAgentContext
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.cached.CachedPromptExecutor
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.message.Message
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printLog

object AgentManager {

    suspend fun quickAgent(input: String) {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        if (apiKey.isNullOrEmpty()) return
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        AIAgent
        val agent = AIAgent(
            promptExecutor = remoteAiExecutor,
            systemPrompt = "You are a helpful assistant. Answer user questions concisely.",
            llmModel = OpenAIModels.Chat.GPT4o,
            temperature = 0.7,
        ) {
            handleEvents {
                onAgentExecutionFailed { ctx ->
                    printLog("err??${ctx.throwable.message}")
                }
            }
        }

        agent.run(input).also { //默认非流式
            printLog("agent>$it")
        }


    }

    suspend fun AIAgentContext.test1(){
       llm.writeSession {
//           prompt.messages //历史消息


           //以下是主动发起请求
           val stream1= requestLLM()
           if (stream1 is Message.Tool.Call){
               //handle tool call
           }

           //
           val stream = requestLLMStreaming()
           stream.collect { chunk-> }
       }
    }

}