@file:OptIn(InternalCompottieApi::class)

package com.hwj.cook.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.extension.requestLLM
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.embeddings.base.Embedder
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.KFile
import com.hwj.cook.buildFileStorage
import com.hwj.cook.createFileMemoryProvider
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_USER_ID
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printE
import io.github.alexzhirkevich.compottie.InternalCompottieApi
import io.github.alexzhirkevich.compottie.createFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/**
 * @author by jason-何伟杰，2025/10/11
 * des:智能体中零散的功能api汇总
 */

/**
 * des:记忆功能是跟存储文件绑定，跟agent无关，这里搞个统一的记忆文件
 */
suspend fun createMemoryProvider(): AgentMemoryProvider {
    val scopeIdFromUserId = getCacheString(DATA_USER_ID, "888")
    return createFileMemoryProvider(scopeIdFromUserId!!)
}

//对应会话的记忆
suspend fun createSessionMemory(sessionId: String): AgentMemoryProvider {
    return createFileMemoryProvider(sessionId)
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
    val flow = remoteAiExecutor.executeStreaming(prompt = prompt, llModel)
    flow.onStart { onStart() }
        .onCompletion { cause: Throwable? ->
            onCompletion(cause)
        }
        .catch { e: Throwable ->
            catch(e)
            printE(e.message, "streaming-err")
        }
        .collect { chunk: StreamFrame ->
            streaming(chunk)
        }
}

suspend fun agentChat() {
    val apiKey = getCacheString(DATA_APP_TOKEN)!!
    val executor = SingleLLMPromptExecutor(buildQwenLLMClient(apiKey))
    executor.use { }
    val chatAgent = AIAgent<String, Unit>(
        systemPrompt = "You're a simple chat agent",
        promptExecutor = executor,
        strategy = functionalStrategy {
            var userResponse = it
            while (userResponse != "/bye") {
                val responses = requestLLM(userResponse)
                println(responses.content)
                userResponse = readln()
            }
        },
        llmModel = buildQwen3LLM()
    )
    chatAgent.run("")
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

fun buildQwen3LLM(id: String = "Qwen/Qwen2.5-7B-Instruct"): LLModel {
    //预设的模型
//    OllamaModels.Alibaba.QWQ_32B   Qwen/Qwen2.5-7B-Instruct  Qwen/Qwen3-VL-8B-Instruct

    //自定义的模型定义
    return LLModel(
        provider = LLMProvider.Alibaba, id, capabilities = listOf(
//            LLMCapability.Tools,
//            LLMCapability.Temperature,
//            LLMCapability.Schema.JSON.Basic
            LLMCapability.Temperature,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Speculation,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,//是会导致请求400？
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

fun buildZaiLLM(): LLModel {
    return LLModel(
        provider = LLMProvider.Anthropic, id = "zai-org/GLM-4.6V", capabilities = listOf(
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
        ), contextLength = 131_072
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


//得搞个向量化映射表，不然没法记录删除
fun buildIndexJson(fileName: String = "index.json"): String {
    val rootDir = createRootDir("embed")

    val indexFile = PlatformFile(PlatformFile(rootDir), fileName)


    return indexFile.absolutePath()
}


suspend fun buildDocumentStorage() {
//    TextFileDocumentEmbeddingStorage //已经封装好文件提取器，偏上层可用
//    FileDocumentEmbeddingStorage //更底层 ，暂时没有到

    buildFileStorage(createRootDir("embed/index"))
}