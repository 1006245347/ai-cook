package com.hwj.cook.agent

import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider
import com.hwj.cook.createFileMemoryProvider
import com.hwj.cook.global.DATA_APPLICATION_NAME

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