package com.hwj.cook.agent

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.printLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

/**
 * @author by jason-何伟杰，2025/7/10
 * des:自家服务器部署openai模型
 * // /baitong/chat/completions   bge-gree  https://baitong-aiw.gree.com/
 */
open class OpenAiRemoteLLMClient(
    apiKey: String, settings: OpenAIClientSettings = OpenAIClientSettings(
        baseUrl = baseHostUrl,
        chatCompletionsPath = "chat/completions",
        embeddingsPath = "embeddings"
    ), baseClient: HttpClient = HttpClient { }.config {
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    }
) : OpenAILLMClient(
    apiKey = apiKey,
    settings = settings,
    baseClient = baseClient
)

fun createAiExecutor(apiKey: String): SingleLLMPromptExecutor {
    return SingleLLMPromptExecutor(
        OpenAILLMClient(
            apiKey = apiKey, settings = OpenAIClientSettings(
                baseUrl = baseHostUrl,
                chatCompletionsPath = "chat/completions",
                embeddingsPath = "embeddings"
            ), baseClient = HttpClient().config {
                install(Logging) {
                    level = LogLevel.ALL
                    logger = object : Logger {
                        override fun log(message: String) {
                            printLog(message)
                        }
                    }
                }
            })
    )
}

