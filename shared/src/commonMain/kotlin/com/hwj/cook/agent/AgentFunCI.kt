package com.hwj.cook.agent

import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.emptyPrompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.streaming.StreamFrame
import com.hwj.cook.createFileMemoryProvider
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import kotlinx.coroutines.flow.Flow
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
    onStart: () -> Unit,
    onCompletion: (Throwable?) -> Unit,
    catch: (Throwable) -> Unit,
    streaming: (StreamFrame) -> Unit
): Flow<StreamFrame> {
    val apiKey = getCacheString(DATA_APP_TOKEN)
    require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
    val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))
    val response =
        remoteAiExecutor.executeStreaming(prompt = prompt, OpenAIModels.Chat.GPT4o)
    response.onStart { onStart() }
        .onCompletion { cause: Throwable? -> onCompletion(cause) }
        .catch { e: Throwable -> catch(e) }
        .collect { chunk: StreamFrame -> streaming(chunk) }
    return response
}