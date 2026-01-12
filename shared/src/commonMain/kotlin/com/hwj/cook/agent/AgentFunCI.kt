package com.hwj.cook.agent

import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.createFileMemoryProvider
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

/**
 * @author by jason-何伟杰，2025/10/11
 * des:智能体中零散的功能api汇总
 */

/**
 * des:记忆功能是跟存储文件绑定，跟agent无关，这里搞个统一的记忆文件
 */
fun createMemoryProvider(): AgentMemoryProvider {
    val scopeIdFromUserId = "888"
    return createFileMemoryProvider(scopeIdFromUserId)
}

//保存的事实是哪类主题subject,属于哪个范围scope,这里的记忆将被所有智能体、功能模块调用，与用户id相关
suspend fun saveMemory(
    memoryProvider: AgentMemoryProvider,
    fact: Fact,
    subject: MemorySubject = MemorySubject.Everything
) {
    memoryProvider.save(
        fact = fact,
        subject = subject,
        scope = MemoryScope.Product(DATA_APPLICATION_NAME)
    )
}

//发现有直接用客户端的流式输出，不需要智能体机制
//放在协程内运行，也能捕捉主动暂停
suspend fun chatStreaming(
    prompt: Prompt,
    llModel: LLModel,
    onStart: suspend () -> Unit,
    onCompletion: suspend (Throwable?) -> Unit,
    catch: suspend (Throwable) -> Unit,
    streaming: suspend (StreamFrame) -> Unit
) {
    val apiKey = getCacheString(DATA_APP_TOKEN)
    require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
    val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))
    val flow =
        remoteAiExecutor.executeStreaming(prompt = prompt, llModel)
    flow.onStart { onStart() }
        .onCompletion { cause: Throwable? -> onCompletion(cause) }
        .catch { e: Throwable ->
            catch(e)
            printD(e.message)
        }
        .collect { chunk: StreamFrame ->
            streaming(chunk)
        }
}

fun buildEditLLM(id: String): LLModel {
    return LLModel(
        provider = if (id.uppercase().contains("qwen")) LLMProvider.Alibaba else LLMProvider.OpenAI,
        id = id, capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.Vision.Image,
            LLMCapability.Document,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Completions,
            LLMCapability.OpenAIEndpoint.Responses
        ), contextLength = 65_536
    )
}

//硅基流动中，有深度思考能力的模型才有enable_thinking字段控制，如  Qwen/Qwen3-8B,他默认启动思考。。
//Qwen/Qwen3-8B //128k 免费

fun buildQwen3LLM_8B(): LLModel {
    return LLModel(
        provider = LLMProvider.Alibaba, id = "Qwen/Qwen3-8B", capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.Vision.Image,
            LLMCapability.Document,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Completions,
            LLMCapability.OpenAIEndpoint.Responses
        ), contextLength = 131_072
    )
}

fun buildQwen3LLM(): LLModel {
    //预设的模型
//    OllamaModels.Alibaba.QWQ_32B

    //自定义的模型定义
    return LLModel(
        provider = LLMProvider.Alibaba, "Qwen/Qwen3-VL-8B-Instruct", capabilities = listOf(
//            LLMCapability.Tools,
//            LLMCapability.Temperature,
//            LLMCapability.Schema.JSON.Basic
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.Vision.Image,
            LLMCapability.Document,
            LLMCapability.Completion,
            LLMCapability.MultipleChoices,
            LLMCapability.OpenAIEndpoint.Completions,
            LLMCapability.OpenAIEndpoint.Responses,
        ), contextLength = 65_536// //32k 32_768   64k 65_536   128k 131_072  1M 1_048_576
    )
}

//嵌入模型
fun buildQwen3EmeLLM(): LLModel {
    return LLModel(
        provider = LLMProvider.Alibaba, id = "Qwen/Qwen3-Embedding-8B", capabilities = listOf(
            LLMCapability.Embed
        ), contextLength = 32_768
    )
}

//处理向量化
fun buildEmbedder(apiKey: String, llModel: LLModel = buildQwen3EmeLLM()): LLMEmbedder {
    val client = OpenAiRemoteLLMClient(
        apiKey = apiKey, settings = OpenAIClientSettings(
            baseUrl = baseHostUrl,
            chatCompletionsPath = "chat/completions",
            embeddingsPath = "embeddings",
        )
    )
    val embedder = LLMEmbedder(client, llModel)
    return embedder
}