package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildIndexJson
import com.hwj.cook.except.NumberFormatter
import com.hwj.cook.models.LocalIndex
import com.hwj.cook.models.RagEvidence
import com.hwj.cook.models.RagResult
import com.hwj.cook.searchRAGChunk
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2026/2/4
 * des:本地知识库检索工具
 * 工业级索引表是多层，多表，多桶chunk,先缩小范围，给出一批chunk集。我们搞个简单的试试
 *
 */
object LocalRagTool : Tool<LocalRagTool.Args, LocalRagTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "LocalRagTool",
    description = "Search query or keywords"
) {

    @Serializable
    data class Args(@property:LLMDescription("从本地知识库检索的相关文本") val query: String)

    @Serializable
    data class Result(val contextString: String)

    override suspend fun execute(args: Args): Result {
        val result = searchRAGChunk(args.query, 0.7, 3)
        val contextString = buildString {
        appendLine("以下是从本地知识库检索到的内容:")

        result.evidence.forEachIndexed { index, item : RagEvidence ->
            appendLine("[$index] 相似度:${NumberFormatter.format(item.similarity,2)}")
            appendLine("来源：${item.payload?.sourcePath}")
            appendLine(item.document) //应该返回切片内容
            appendLine()
        }
    }
        return Result(contextString)
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