@file:OptIn(ExperimentalTime::class)

package com.hwj.cook.agent

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.hwj.cook.global.printLog
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import kotlin.time.ExperimentalTime

/**
 * @author by jason-何伟杰，2025/7/10
 * des:自家服务器部署openai模型
 * // /baitong/chat/completions   bge-gree  https://baitong-aiw.gree.com/
 */
open class OpenAiRemoteLLMClient(
    apiKey: String, settings: OpenAIClientSettings = OpenAIClientSettings(
        baseUrl = "https://baitong-it.gree.com",
        chatCompletionsPath = "aicodeOpen/baitong/chat/completions",
        embeddingsPath = "https://baitong-aiw.gree.com/openapi/v2/embeddings"
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
) : OpenAILLMClient(apiKey, settings, baseClient) //kotlin.time.Clock

fun createAiExecutor(apiKey: String): SingleLLMPromptExecutor {
    return SingleLLMPromptExecutor(
        OpenAILLMClient(
            apiKey = apiKey, settings = OpenAIClientSettings(
                baseUrl = "https://baitong-it.gree.com",
                chatCompletionsPath = "aicodeOpen/baitong/chat/completions",
                embeddingsPath = "https://baitong-aiw.gree.com/openapi/v2/embeddings"
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

