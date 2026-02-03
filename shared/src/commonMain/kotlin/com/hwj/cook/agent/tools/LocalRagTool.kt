package com.hwj.cook.agent.tools

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.rag.base.files.DocumentProvider
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildIndexJson
import com.hwj.cook.models.LocalIndex
import io.github.alexzhirkevich.compottie.createFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import kotlinx.io.files.Path
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class LocalRagTool<Document, Path>(
    private val embedder: LLMEmbedder,
    private val documentProvider: DocumentProvider<Document, Path>
) {


    suspend fun search(query: String) {


    }
}


//要全局处理documentId的索引表，不然
suspend fun findIndexDocumentId(filePath: String): String? {
    val indexFilePath = buildIndexJson()
    if (!PlatformFile(indexFilePath).exists()) {
        return null
    }
    val json = PlatformFile(indexFilePath).readString() //读取文件内容
    if (!json.isEmpty()) {
        val indexRoot: LocalIndex = JsonApi.decodeFromString<LocalIndex>(json)
        val item = indexRoot.indexedFiles?.firstOrNull { it.filePath == filePath }
        return item?.documentId
    }
    return null
}