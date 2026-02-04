package com.hwj.cook.data.repository

import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildEmbedder
import com.hwj.cook.agent.buildIndexJson
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.buildFileStorage
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_BOOK_ROOT
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.getMills
import com.hwj.cook.global.printD
import com.hwj.cook.global.printList
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.IndexFile
import com.hwj.cook.models.LocalIndex
import com.hwj.cook.storeFile
import io.github.alexzhirkevich.compottie.InternalCompottieApi
import io.github.alexzhirkevich.compottie.createFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.writeString
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

/**
 * @author by jason-何伟杰，2026/2/4
 * des:将所有本地菜谱向量化
 */
@OptIn(InternalCompottieApi::class)
object CookBookRepository {

    // /data/user/0/com.hwj.cook.android/files/.aicook/files/resource/dishes/vegetable_dish/松仁玉米.md
    suspend fun loadCookBookVector(node: BookNode?) {
        val rootBook = getCacheString(DATA_BOOK_ROOT)
        if (rootBook.isNullOrEmpty() || node == null) return


        val bookList = node.collectFilePaths()
        if (bookList.isEmpty()) return
        val llmEmbedder = buildEmbedder(getCacheString(DATA_APP_TOKEN)!!)
        buildFileStorage(createRootDir("embed/cook"), llmEmbedder)

        val indexRootPath = buildIndexJson("cook.json")
        if (!PlatformFile(indexRootPath).exists()) {
            FileSystem.SYSTEM.createFile(indexRootPath.toPath())
        }

        val embedList = mutableListOf<IndexFile>()
        var sort: Long = -1
        bookList.forEach {
            printD("${(++sort)}" +it.realPath)
                storeFile(it.realPath) { documentId ->  //向量处理后的回调documentId
                    val srcFile = PlatformFile(it.realPath)
                    val itemIndex = IndexFile(
                        documentId = documentId,
                        absolutePath = it.realPath,
                        fileName = srcFile.name,
                        filePath = srcFile.absolutePath(),
                        fileType = srcFile.extension,
                        fileSize = srcFile.size(),
                        millDate = getMills(),
                        sort = ++sort,
                        tag = setSelectTag(it.realPath),//期望搞个索引分类
                        isEmbed = true,
                    )
                    embedList.add(itemIndex)
                }
        }


        //生成cook.json
        val json = PlatformFile(indexRootPath).readString()
        if (!json.isEmpty()) {
            val indexRoot = JsonApi.decodeFromString<LocalIndex>(json)
            val indexFiles = indexRoot.indexedFiles
            if (indexFiles == null || indexFiles.isEmpty()) {
                indexRoot.indexedFiles = embedList
            } else {
                indexFiles.addAll(embedList)
            }
            val cache = JsonApi.encodeToString(indexRoot)
            PlatformFile(indexRootPath).writeString(cache)
        } else {
            val indexRoot = LocalIndex()
            indexRoot.indexedFiles = embedList
            val cache = JsonApi.encodeToString(indexRoot)
            PlatformFile(indexRootPath).writeString(cache)
        }
    }

    //一条一条加，有点拉
    suspend fun buildCookIndexJson(srcPath: String, documentId: String) {
        val indexRootPath = buildIndexJson("cook.json")
        if (!PlatformFile(indexRootPath).exists()) {
            FileSystem.SYSTEM.createFile(indexRootPath.toPath())
        }

        val json = PlatformFile(indexRootPath).readString()
        val srcFile = PlatformFile(srcPath)
        val itemIndex = IndexFile(
            documentId = documentId,
            absolutePath = srcPath,
            fileName = srcFile.name,
            filePath = srcFile.absolutePath(),
            fileType = srcFile.extension,
            fileSize = srcFile.size(),
            millDate = getMills(),
            isEmbed = true,
            fileHash = null
        )
        if (!json.isEmpty()) {
            val indexRoot = JsonApi.decodeFromString<LocalIndex>(json)
            val indexFiles = indexRoot.indexedFiles
            if (indexFiles == null || indexFiles.isEmpty()) {
                indexRoot.indexedFiles = mutableListOf(itemIndex)
            } else {
                indexFiles.add(itemIndex)
            }
            val cache = JsonApi.encodeToString(indexRoot)
            PlatformFile(indexRootPath).writeString(cache)
        } else {
            val indexRoot = LocalIndex()
            indexRoot.indexedFiles = mutableListOf(itemIndex)
            val cache = JsonApi.encodeToString(indexRoot)
            PlatformFile(indexRootPath).writeString(cache)
        }
    }


    fun BookNode.collectFilePaths(): List<BookNode> {
        val result = mutableListOf<BookNode>()

        fun dfs(node: BookNode) {
            if (node.isDirectory) {
                node.children.forEach { dfs(it) }
            } else {
                if (!node.realPath.contains("starsystem")) {
                    result += node
                }
            }
        }

        dfs(this)
        return result
    }

    fun setSelectTag(text: String): String? {
        val label = text.lowercase()
        return if (label.contains("star")) {
            "star"
        } else if (label.contains("tips")) {
            "method"
        } else {
            "recipe"
        }
    }
}