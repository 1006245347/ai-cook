package com.hwj.cook

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.rag.base.files.FileSystemProvider
import ai.koog.rag.vector.TextFileDocumentEmbeddingStorage
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.OsStatus
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import com.hwj.cook.models.RagResult
import io.ktor.client.HttpClient
import io.ktor.http.HeadersBuilder

interface Platform {

    val name: String
    val os: OsStatus
}

expect fun getPlatform(): Platform

expect fun createKtorHttpClient(timeout: Long?, builder: HeadersBuilder.() -> Unit): HttpClient

@Composable
expect fun setColorScheme(isDark: Boolean): ColorScheme

expect fun checkSystem(): OsStatus

//手机权限
@Composable
expect fun createPermission(
    vararg permissions: Any,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
)

expect fun listResourceFiles(path: String): BookNode?
expect fun readResourceFile(path: String): String?
expect fun loadZipRes(): String?

expect fun createFileMemoryProvider(scopeId: String = ""): AgentMemoryProvider
expect fun getDeviceInfo(): DeviceInfoCell

//这样设计是为了保持使用jvm的ToolSet,却忽略iOS的ToolSet实现(根本就没有)
//这种做工具只能显示描述，不能用注解反射
expect interface PlatformToolSet
expect interface KmpToolSet : PlatformToolSet

//各个平台特属的工具
expect fun platformAgentTools(): ToolRegistry

//各个平台特属的智能体 累加
expect fun plusAgentList(): List<AgentInfoCell>

//用来测试
expect suspend fun runLiteWork(call: () -> Unit)

//用来测试
@Composable
expect fun demoUI(content: @Composable () -> Unit)

@Composable
expect fun BoxScope.scrollBarIn(state: ScrollState)

//为了令向量写入IO通用，定义个中介文件对象
expect class KFile {
    val name: String
    val extension: String
    val absolutePath: String
    fun resolve(child: String): KFile
    fun parent(): KFile?
    suspend fun readText(): String
    suspend fun readLines(): List<String>
    suspend fun writeText(text: String)
    suspend fun writeLines(lines: List<String>)
}

//保留jvm的Provider,内部的文件提取都是java.Path没法修改，所以iOS用了KFile
//filePath是保存向量文件的根目录
expect suspend fun buildFileStorage(filePath: String,embedder:LLMEmbedder)
expect suspend fun storeFile(filePath: String, callback: (String?) -> Unit)
expect suspend fun deleteRAGFile(documentId: String)

expect suspend fun buildChunkStorage(path: String, callback: (List<String>) -> Unit)
expect suspend fun searchRAGChunk(query: String,
                                  similarityThreshold: Double ,
                                  topK: Int ): RagResult
