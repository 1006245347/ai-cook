package com.hwj.cook.agent

import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.Storage
import ai.koog.rag.base.files.FileSystemProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.Fact
import ai.koog.agents.memory.model.MemoryScope
import ai.koog.agents.memory.model.MemorySubject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.SerializationException
import platform.Foundation.NSFileManager
import platform.Foundation.NSLog

/**
 * @author by jason-何伟杰，2025/10/13
 * des:类似jvm下的LocalFileMemoryProvider
 */
class IOSFileMemoryProvider(
    private val config: LocalMemoryConfig,
    private val storage: Storage<String>,
    private val fs: FileSystemProvider.ReadWrite<String>,
    private val root: String
) : AgentMemoryProvider {


    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun getStoragePath(subject: MemorySubject, scope: MemoryScope): String {
        val segments = listOf(config.storageDirectory) + when (scope) {
            is MemoryScope.Agent -> listOf("agent", scope.name, "subject", subject.name)
            is MemoryScope.Feature -> listOf("feature", scope.id, "subject", subject.name)
            is MemoryScope.Product -> listOf("product", scope.name, "subject", subject.name)
            MemoryScope.CrossProduct -> listOf("organization", "subject", subject.name)
        }
        return segments.fold(root) { acc, segment -> fs.joinPath(acc, segment) }
    }

    private suspend fun loadFacts(path: String): Map<String, List<Fact>> = mutex.withLock {
        val content = storage.read(path) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, List<Fact>>>(content)
        } catch (e: SerializationException) {
            NSLog("Failed to deserialize facts from $path: ${e.message}")
            emptyMap()
        } catch (e: Exception) {
            NSLog("Unexpected error loading facts from $path: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun saveFacts(path: String, facts: Map<String, List<Fact>>) = mutex.withLock {
        val serialized = json.encodeToString(facts)
        storage.write(path, serialized)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun save(fact: Fact, subject: MemorySubject, scope: MemoryScope) {
        val path = getStoragePath(subject, scope)
        // iOS 创建目录
        val dirPath = path.deletingLastPathComponent()
        NSFileManager.defaultManager.createDirectoryAtPath(
            dirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        val facts = loadFacts(path).toMutableMap()
        val key = fact.concept.keyword
        facts[key] = (facts[key] ?: emptyList()) + fact

        saveFacts(path, facts)
    }

    override suspend fun load(
        concept: Concept,
        subject: MemorySubject,
        scope: MemoryScope
    ): List<Fact> {
        val path = getStoragePath(subject, scope)
        val facts = loadFacts(path)
        return facts[concept.keyword] ?: emptyList()
    }

    override suspend fun loadAll(subject: MemorySubject, scope: MemoryScope): List<Fact> {
        val path = getStoragePath(subject, scope)
        return loadFacts(path).values.flatten()
    }

    override suspend fun loadByDescription(
        description: String,
        subject: MemorySubject,
        scope: MemoryScope
    ): List<Fact> {
        val path = getStoragePath(subject, scope)
        val facts = loadFacts(path)
        return facts.values.flatten().filter { fact ->
            fact.concept.description.contains(description, ignoreCase = true)
        }
    }

    private fun String.deletingLastPathComponent(): String {
        val idx = this.lastIndexOf('/')
        return if (idx > 0) this.substring(0, idx) else this
    }

}