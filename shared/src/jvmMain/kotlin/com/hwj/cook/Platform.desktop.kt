package  com.hwj.cook

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.Aes256GCMEncryptor
import ai.koog.agents.memory.storage.EncryptedStorage
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.rag.base.files.JVMDocumentProvider
import ai.koog.rag.base.files.JVMFileSystemProvider
import ai.koog.rag.vector.EmbeddingBasedDocumentStorage
import ai.koog.rag.vector.FileDocumentEmbeddingStorage
import ai.koog.rag.vector.FileVectorStorage
import ai.koog.rag.vector.JVMTextFileDocumentEmbeddingStorage
import ai.koog.rag.vector.TextDocumentEmbedder
import ai.koog.rag.vector.TextFileDocumentEmbeddingStorage
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwj.cook.agent.JvmChunkDocumentProvider
import com.hwj.cook.agent.buildEmbedder
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.agent.rag.FileChunk
import com.hwj.cook.agent.rag.chunkFile
import com.hwj.cook.agent.tools.SwitchTools
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.bookShouldIgnore
import com.hwj.cook.global.cBasic
import com.hwj.cook.global.cBlue244260FF
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.getFileLabel
import com.hwj.cook.global.printD
import com.hwj.cook.global.printList
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import com.hwj.cook.models.RagEvidence
import com.hwj.cook.models.RagPayload
import com.hwj.cook.models.RagResult
import com.hwj.cook.models.SuggestCookSwitch
import com.hwj.cook.ui.StreamingChatScreen
import com.sun.management.OperatingSystemMXBean
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.headers
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

class DesktopPlatform : Platform {
    override val name: String
        get() = "desktop> ${System.getProperty("os.name")}"
    override val os: OsStatus
        get() = checkSystem()

}

actual fun getPlatform(): Platform = DesktopPlatform()

actual fun checkSystem(): OsStatus {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> OsStatus.MACOS
        os.contains("win") -> OsStatus.WINDOWS
        os.contains("nix") || os.contains("nux") || os.contains("ubu") -> OsStatus.LINUX
        else -> OsStatus.UNKNOWN
    }
}

@Composable
actual fun setColorScheme(isDark: Boolean): ColorScheme {
    return if (!isDark) {
        LightColorScheme
    } else {
        DarkColorScheme
    }
}

@Composable
actual fun createPermission(
    vararg permissions: Any,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    grantedAction()
}

actual fun createKtorHttpClient(timeout: Long?, builder: HeadersBuilder.() -> Unit): HttpClient {
    return HttpClient {
        defaultRequest {
            url.takeFrom(URLBuilder().takeFrom(baseHostUrl))
            headers(builder)
        }
        install(HttpTimeout) {
            timeout?.let {
//                requestTimeoutMillis = 60000  //从发出请求到结束，总共最多等多久
//                connectTimeoutMillis = 10000 //连服务器连不上，多久放弃
//                socketTimeoutMillis = 5000 //已经连上了，但多久没收到数据就放弃

                requestTimeoutMillis = 60000 * 5  //从发出请求到结束，总共最多等多久
                connectTimeoutMillis = 10000 //连服务器连不上，多久放弃
                socketTimeoutMillis = timeout //30秒没token
            }
        }
        install(SSE) {
//            showCommentEvents()
//            showRetryEvents()
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(Logging) {
//                level = LogLevel.ALL
//            level = LogLevel.INFO //接口日志屏蔽
            level = LogLevel.NONE
            logger = object : Logger {
                override fun log(message: String) {
                    printD(message)
//                    println(message)
                }
            }
        }

        expectSuccess = true
    }.also { client ->
        client.plugin(HttpSend).intercept { request ->
            request.headers.append(
                HttpHeaders.Authorization,
                "Bearer ${getCacheString(DATA_MCP_KEY)}"
            )
            execute(request)
        }
    }
}

//这里有个坑，zipStream只处理utf-8文件，7-zip压缩包.zip编码参数 cu=on
actual fun loadZipRes(): String? {
    try {
        val folder = System.getProperty("user.home") + "/.aicook/files"
        val target = File(folder)
        if (!target.exists()) target.mkdirs()
        val zipStream = //换个压缩格式，不然文件有编码问题
            object {}.javaClass.getResourceAsStream("/resource.zip") ?: error("not found hwj")
        unzipResource(zipStream, target.absolutePath)
        return target.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun unzipResource(zipStream: InputStream, targetDir: String) {
    val target = File(targetDir)
    ZipInputStream(zipStream).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(target, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile.mkdirs()
                outFile.outputStream().use { out -> zis.copyTo(out) }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

//("应用运行时resource的资源是jar打包在应用内，无法直接获取")
actual fun listResourceFiles(path: String): BookNode? {
    fun makeNode(f: File): BookNode? {
        if (bookShouldIgnore(f.absolutePath)) return null
        return if (f.isDirectory) {
            BookNode(
                name = f.name,
                isDirectory = true, realPath = f.absolutePath,
                loader = {
                    f.listFiles()?.mapNotNull { makeNode(it) } ?: emptyList()
                }
            )
        } else {
            BookNode(name = f.name, isDirectory = false, realPath = f.absolutePath)
        }
    }
    try {
        val rootFile = File(path, "resource")
        return makeNode(rootFile)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

//读取打包在应用内部的文档
actual fun readResourceFile(path: String): String? {
    return File(path).readText()
}

actual fun createFileMemoryProvider(scopeId: String): AgentMemoryProvider {
    return LocalFileMemoryProvider(
        config = LocalMemoryConfig("memory-cache/$scopeId"),
        storage = EncryptedStorage(
            fs = JVMFileSystemProvider.ReadWrite,
            encryption = Aes256GCMEncryptor("7UL8fsTqQDq9siUZgYO3bLGqwMGXQL4vKMWMscKB7Cw=")
        ),
        fs = JVMFileSystemProvider.ReadWrite,
        root = Path(createRootDir())
    )
}

actual fun getDeviceInfo(): DeviceInfoCell {
    getPlatform().name
    val osBean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
    return DeviceInfoCell(
        cpuCores = osBean.availableProcessors,
        cpuArch = System.getProperty("os.arch"),
        totalMemoryMB = osBean.totalPhysicalMemorySize / (1024 * 1024),
        brand = System.getProperty("os.name"),
        model = System.getProperty("user.name"),
        osVersion = System.getProperty("os.version"),
        platform = getPlatform().os.name
    )
}

actual interface KmpToolSet : PlatformToolSet
actual typealias PlatformToolSet = ToolSet

actual fun platformAgentTools(): ToolRegistry {
    return ToolRegistry {
        SwitchTools(SuggestCookSwitch()).asTools()
    }
}

actual fun plusAgentList(): List<AgentInfoCell> {
    val list = mutableListOf<AgentInfoCell>()
    return list
}

actual suspend fun runLiteWork(call: () -> Unit) {
//    McpClientUtils.searchMcpClientSSE("today is ?",getCacheString(DATA_MCP_KEY)!!)
//    McpClientUtils.tryClient()
//    McpClientUtils.testMcp()

//    searchRag("部门人数多少？")
//    buildChunkStorage()
//    searchRAGChunk("RAG 系统由两个核心模块组成")
    call()
}

@Composable
actual fun demoUI(content: @Composable () -> Unit) {
    StreamingChatScreen()
    content()
}

//官方只有jvm才有滚动条，mobile没搞
@Composable
actual fun BoxScope.scrollBarIn(state: ScrollState) {
    val barStyle = ScrollbarStyle(
        minimalHeight = 16.dp,
        shape = RoundedCornerShape(3.dp),
        thickness = 4.dp,
        hoverDurationMillis = 300,
        unhoverColor = cBasic(),
        hoverColor = cBlue244260FF()
    )
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state), style = barStyle,
        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
    )
}

actual class KFile(val path: Path) {
    actual val name: String get() = path.name
    actual val extension: String
        get() = path.fileName
            ?.toString()
            ?.substringAfterLast('.', "")
            ?: ""
    actual val absolutePath: String
        get() = path.absolutePathString()

    actual fun resolve(child: String): KFile {
        return KFile(path.resolve(child))
    }

    actual fun parent(): KFile? =
        path.parent?.let { KFile(it) }

    actual suspend fun readText(): String = path.readText()
    actual suspend fun readLines(): List<String> = path.readLines()
    actual suspend fun writeText(text: String) = path.writeText(text)
    actual suspend fun writeLines(lines: List<String>) {
        path.writeLines(lines)
    }
}

//这是存一个完整的文件的，没有对文件进行切片
lateinit var storageProvider: TextFileDocumentEmbeddingStorage<Path, Path>

//JVMDocumentProvider 内部支持java.nio.file.Path,导致
actual suspend fun buildFileStorage(filePath: String, embedder: LLMEmbedder) {
    val mFile = Path(filePath)
    val storage = TextFileDocumentEmbeddingStorage(
        embedder,
        JVMDocumentProvider,
        JVMFileSystemProvider.ReadWrite,
        mFile
    )
    storageProvider = storage
}


//不知道如何处理storageProvider的类型，搞了个全局变量
actual suspend fun storeFile(filePath: String, callback: (String?) -> Unit) {
    val id = storageProvider.store(Path(filePath))
    callback(id)
}

actual suspend fun deleteRAGFile(documentId: String) {
    storageProvider.delete(documentId)
}


suspend fun searchRag(query: String, similarityThreshold: Double = 0.0, topK: Int = 3): RagResult {
//    storageProvider.apply { //拥有的能力api
//        rankDocuments()
//        read()
//        store()
//        delete()
//        read()
//    }


    //相关性排序
//    val rankedFiles = storageProvider.rankDocuments(query).toList()
    //最相关的文档
//    val tops = storageProvider.mostRelevantDocuments(query, count = topK, similarityThreshold)
//    tops.forEach { p ->
//        printD(">>>>$p")
//    }

//    JVMTextFileDocumentEmbeddingStorage(buildEmbedder(""),createRootDir("emb"))

    val list1 = storageProvider.rankDocuments(query)
        .filter { it.similarity >= similarityThreshold }
        .toList()
        .sortedByDescending { it.similarity }
        .take(topK) //顶部3个
        .map {

            //这里只返回Path(文件)，没有分片哪行的处理，期望返回切片chunkIndex
            RagEvidence(it.document.absolutePathString(), payload = null, it.similarity)
        }.toList()

    val contextString = buildString {
        appendLine("以下是从本地知识库检索到的内容:")
        list1.forEachIndexed { index, item ->
            appendLine("[$index] 相似度:${"%.2f".format(item.similarity)}")
            appendLine("来源：${item.document}")
//            appendLine(item.document) //应该返回切片内容
            appendLine()
        }
    }

    printD(contextString)
    return RagResult(query = query, evidence = list1)
}

lateinit var chunkStorageProvider: FileDocumentEmbeddingStorage<FileChunk, Path>

actual suspend fun buildChunkStorage(path: String, callback: (List<String>) -> Unit) {
//suspend fun buildChunkStorage() {
//    val path = "/Users/jasonmac/Documents/androidstudy/rag.txt"
    val apiKey = getCacheString(DATA_APP_TOKEN)
    val embedder1 = buildEmbedder(apiKey!!)
    val embedder2 = TextDocumentEmbedder(JvmChunkDocumentProvider, embedder1)
//    val root = createRootDir("embed/index/chunk/${getFileLabel(path)}")
    val root = createRootDir("embed/index/chunk/rag")
    chunkStorageProvider = FileDocumentEmbeddingStorage<FileChunk, Path>(
        embedder = embedder2, documentProvider = JvmChunkDocumentProvider,
        fs = JVMFileSystemProvider.ReadWrite, root = Path(root)
    )

//    chunkStorageProvider = TextFileDocumentEmbeddingStorage(
//        embedder = embedder1,
//        JvmChunkDocumentProvider, fs = JVMFileSystemProvider.ReadWrite, root = Path(root)
//    )


    val chunks = chunkFile(kotlinx.io.files.Path(path)) //段落切片

    val listId = mutableListOf<String>()
    chunks.forEachIndexed { index, chunk ->
        val id = chunkStorageProvider.store(chunk)
//        printD("id>$id")
        listId.add(id)
    }
    callback(listId)
}

actual suspend fun searchRAGChunk(
    query: String,
    similarityThreshold: Double ,
    topK: Int
): RagResult {
    val apiKey = getCacheString(DATA_APP_TOKEN)
    val embedder1 = buildEmbedder(apiKey!!)
    val embedder2 = TextDocumentEmbedder(JvmChunkDocumentProvider, embedder1)
    val root = createRootDir("embed/index/chunk/rag")
    chunkStorageProvider = FileDocumentEmbeddingStorage<FileChunk, Path>(
        embedder = embedder2, documentProvider = JvmChunkDocumentProvider,
        fs = JVMFileSystemProvider.ReadWrite, root = Path(root)
    )
//    JvmChunkDocumentProvider.document() //这函数只有在检索才调用
    val list = chunkStorageProvider.rankDocuments(query)
        .filter { it.similarity >= similarityThreshold }
        .toList()
        .sortedByDescending { it.similarity }
        .take(topK) //顶部3个
        .map { //返回的是  RankedDocument<Document>
            RagEvidence(
                document = Path(it.document.text).readText(), payload = RagPayload(
                    documentId = "id", chunkIndex = it.document.index,
                    sourcePath = it.document.path.toString()
                ), similarity = it.similarity
            )
        }
        .toList()
    val contextString = buildString {
        appendLine("以下是从本地知识库检索到的内容:")
        list.forEachIndexed { index, item: RagEvidence ->
            appendLine("[$index] 相似度:${"%.2f".format(item.similarity)}")
            appendLine("来源：${item.payload?.sourcePath}")
            appendLine(item.document) //应该返回切片内容
            appendLine()
        }
    }
//    printD("c>$contextString")
    return RagResult(query = query, evidence = list)
}

private suspend fun c() {
    //检索相关文件
    val path = JVMDocumentProvider.document(Path(""))
    //检索指定的文件内容
//    JVMDocumentProvider.textFragment()
//    JVMDocumentProvider的作用就是将Path引导 将路径转化为文件内容解析
    val documentEmbedder = TextDocumentEmbedder(JVMDocumentProvider, buildEmbedder(""))
    val vectorStorage = EmbeddingBasedDocumentStorage(
        documentEmbedder, FileVectorStorage(
            JVMDocumentProvider,
            JVMFileSystemProvider.ReadWrite, Path("s")
        )
    ) //等同于FileDocumentEmbeddingStorage


//    vectorStorage.mostRelevantDocuments() //相识度
//     JVMTextFileDocumentEmbeddingStorage //用这个不就好了。。。、

//   val ss= JVMFileVectorStorage(Path(""))
//    ss.store(Path(""), Vector(listOf())) //直接存文件路径并绑定它内容的向量集
    val sss = JVMTextFileDocumentEmbeddingStorage(buildEmbedder(""), Path(""))
//    sss.store(Path(""))
//    sss.store()

    sss.store(Path(""))
//    sss.rankDocuments("").filter { it.similarity >= 0.5f }
//        .toList()
//        .sortedByDescending { it.similarity }
//        .take(3) //顶部3个
//        .map{
//            RagEvidence(it.document,it.similarity)
//        }
}