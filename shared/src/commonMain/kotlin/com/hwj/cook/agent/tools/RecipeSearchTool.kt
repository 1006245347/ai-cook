package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildIndexJson
import com.hwj.cook.except.NumberFormatter
import com.hwj.cook.fastSearchIndexContent
import com.hwj.cook.global.printD
import com.hwj.cook.global.printList
import com.hwj.cook.models.IndexFile
import com.hwj.cook.models.LocalIndex
import com.hwj.cook.models.RagEvidence
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2026/2/5
 * des:菜谱检索工具
 */
object RecipeSearchTool : Tool<RecipeSearchTool.Args, RecipeSearchTool.Result>(
    argsSerializer = Args.serializer(),
    resultSerializer = Result.serializer(),
    name = "RecipeSearchTool",
    description = """
        Retrieve semantically relevant cooking recipes from a recipe knowledge base.

        This tool supports natural language recipe discovery based on ingredients,
        cuisines, cooking methods, dietary preferences, and meal scenarios.
        It performs semantic similarity search rather than keyword matching.

        Use this tool when the user is looking for recipe ideas or specific dishes.
        The retrieved content should be used as contextual reference material,
        not returned directly to the user.
    """.trimIndent()
) {

    @Serializable
    data class Args(
        @property:LLMDescription(
            "A natural language description of the recipe or dish the user is looking for. " +
                    "The query may include ingredients, cooking methods, cuisines, dietary constraints, " +
                    "or meal context. The input does not need to be precise or structured."
        ) val query: String
    )

    @Serializable
    data class Result(val contextString: String)

    override suspend fun execute(args: Args): Result {
        val candidateFiles = findIndexBook(args.query)
        printList(candidateFiles,"search?????")
        val ids = candidateFiles?.map { it.documentId!! }?.toList()

        val ragResult = fastSearchIndexContent(query = args.query, ids, 0.6)

        val contextString = buildString {
            if (ragResult?.evidence.isNullOrEmpty()) {
                appendLine("Can not find any thing from the knowledge base:")
            } else {
                appendLine("The following recipes are retrieved from the knowledge base:")
            }
            ragResult?.evidence?.forEachIndexed { index, item: RagEvidence ->
                appendLine("[$index] Similarity:${NumberFormatter.format(item.similarity, 2)}")
                appendLine("Source：${item.payload?.sourcePath}")
                appendLine(PlatformFile(item.document).readString()) //应该返回切片内容
                appendLine()
            }
        }
//        printD("contextString??????$contextString")
        return Result(contextString)
    }
}

//返回相关的索引项 集合
suspend fun findIndexBook(query: String): List<IndexFile>? {
    val indexRootPath = buildIndexJson("cook.json")
    if (!PlatformFile(indexRootPath).exists()) return null
    val json = PlatformFile(indexRootPath).readString()
    if (!json.isEmpty()) {
        val q = query.lowercase()
        val indexRoot = JsonApi.decodeFromString<LocalIndex>(json)
        val candidateFiles = indexRoot.indexedFiles?.filter { file ->
            q.contains(file.fileName!!.removeSuffix(".md").lowercase())
                    || file.tag != null && q.contains(file.tag.lowercase())
                    || q.contains("技巧") && file.filePath!!.contains("tips")
                    || q.contains("使用") && file.filePath!!.contains("tips")
                    || q.contains("学习") && file.filePath!!.contains("tips")
        }

        return candidateFiles
    }
    return null
}
