package com.hwj.cook

import ai.koog.agents.core.tools.Tool
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
import ai.koog.rag.vector.FileDocumentEmbeddingStorage
import ai.koog.rag.vector.TextDocumentEmbedder
import ai.koog.rag.vector.TextFileDocumentEmbeddingStorage
import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.hwj.cook.agent.JvmChunkDocumentProvider
import com.hwj.cook.agent.buildEmbedder
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.agent.rag.FileChunk
import com.hwj.cook.agent.rag.chunkFile
import com.hwj.cook.agent.tools.SwitchTools
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.MainApplication
import com.hwj.cook.global.OsStatus
import com.hwj.cook.global.askPermission
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printLog
import com.hwj.cook.global.purePermission
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import com.hwj.cook.models.RagEvidence
import com.hwj.cook.models.RagPayload
import com.hwj.cook.models.RagResult
import com.hwj.cook.models.SuggestCookSwitch
import com.permissionx.guolindev.PermissionX
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import dev.icerock.moko.permissions.storage.STORAGE
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.utils.toPath
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
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
import okio.Path.Companion.toPath
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlin.reflect.jvm.jvmName

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val os: OsStatus
        get() = OsStatus.ANDROID
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun createKtorHttpClient(timeout: Long?, builder: HeadersBuilder.() -> Unit): HttpClient {
    return HttpClient {
        defaultRequest {
            url.takeFrom(URLBuilder().takeFrom(baseHostUrl))
            headers(builder)
        }

        install(HttpTimeout) {
            timeout?.let { //sse 跟普通接口不同
//                requestTimeoutMillis = 60000  //从发出请求到结束，总共最多等多久
//                connectTimeoutMillis = 10000 //连服务器连不上，多久放弃
//                socketTimeoutMillis = 5000 //已经连上了，但多久没收到数据就放弃

                requestTimeoutMillis = 60000 * 5  //从发出请求到结束，总共最多等多久
                connectTimeoutMillis = 20000 //连服务器连不上，多久放弃
                socketTimeoutMillis = timeout //30秒没token
            }
        }
        install(SSE) {
            showCommentEvents()
            showRetryEvents()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
//            level = LogLevel.Body
//            level = LogLevel.HEADERS
//            level = LogLevel.NONE //接口日志屏蔽
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    printD(message, des = "http-android>")
                }
            }
        }
        //允许分块处理
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

actual fun checkSystem(): OsStatus {
    return OsStatus.ANDROID
}

@Composable
actual fun setColorScheme(isDark: Boolean): ColorScheme {
    var colorScheme = LightColorScheme
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (isDark) {
            colorScheme = dynamicDarkColorScheme(context)
        } else {
            colorScheme = dynamicLightColorScheme(context)
        }
    } else {
        if (isDark) {
            colorScheme = DarkColorScheme
        } else {
            colorScheme = LightColorScheme
        }
    }

    //优化黑白主题下状态栏颜色
    val view = LocalView.current
    DisposableEffect(isDark) {
        val window = (view.context as ComponentActivity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDark
        onDispose { }
    }
    return colorScheme
}

@Composable
actual fun createPermission(
    vararg permissions: Any, //PermissionPlatform
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    val requestList = mutableListOf<String>()
    permissions.forEach { permission ->
        val p = when (permission) {
            PermissionPlatform.CAMERA -> Manifest.permission.CAMERA
            PermissionPlatform.GALLERY -> Permission.GALLERY
            PermissionPlatform.STORAGE -> Permission.STORAGE
            PermissionPlatform.REMOTE_NOTIFICATION -> Permission.REMOTE_NOTIFICATION
            else -> Permission.STORAGE
        }
        if (permission == PermissionPlatform.GALLERY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestList.add(Manifest.permission.READ_MEDIA_IMAGES)
                requestList.add(Manifest.permission.READ_MEDIA_VIDEO)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else if (permission == PermissionPlatform.STORAGE) { //包含读写
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //android10分区存储，无法访问其他应用文件夹
                requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            requestList.add(p.toString())
        }
    }
//    askPermission(p, grantedAction, deniedAction) //也能用，但是喜欢原生
    PermissionX.init(LocalActivity.current as FragmentActivity)
        .permissions(requestList)
        .onExplainRequestReason { scope, deniedList ->
            val tip = "应用需要以下权限:"
            scope.showRequestReasonDialog(deniedList, tip, "ok", "cancel")
        }.request { allGranted, grantedList, deniedList ->
            if (allGranted) {
                grantedAction()
            } else {
                deniedAction()
            }
        }
}

actual fun loadZipRes(): String? {
    try { //设置解压的目标地址
        val zipPath = File(MainApplication.appContext.filesDir, "files").apply {
            if (!exists()) mkdirs()
        }.absolutePath
        val target = File(zipPath)

        //安卓只能用assets导资源
        val zipStream = MainApplication.appContext.assets.open("resource.zip")
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
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { out -> zis.copyTo(out) }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}

actual fun listResourceFiles(path: String): BookNode? {
    fun makeNode(f: File): BookNode {
//        printLog("??>${f.isDirectory} ${f.absolutePath}")
        return if (f.isDirectory) {
            BookNode(
                name = f.name,
                isDirectory = true, realPath = f.absolutePath,
                loader = {
                    f.listFiles()?.map { makeNode(it) } ?: emptyList()
                }
            )
        } else {
            BookNode(
                name = f.name,
                isDirectory = false,
                realPath = f.absolutePath
            ) //不对，存的父文件路径?  对的
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
    val am =
        MainApplication.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo().apply { am.getMemoryInfo(this) }

    val cpuArch = System.getProperty("os.arch")
    val cores = Runtime.getRuntime().availableProcessors()

    return DeviceInfoCell(
        cpuCores = cores,
        cpuArch = cpuArch,
        totalMemoryMB = memInfo.totalMem / (1024 * 1024),
        brand = Build.BRAND,
        model = Build.MODEL,
        osVersion = "Android ${Build.VERSION.RELEASE}",
        platform = "Android"
    )
}


actual interface KmpToolSet : PlatformToolSet
actual typealias PlatformToolSet = ToolSet

//jvm可用的tool
actual fun platformAgentTools(): ToolRegistry {
    return ToolRegistry {
        SwitchTools(SuggestCookSwitch()).asTools()
    }
}

actual fun plusAgentList(): List<AgentInfoCell> {
    return listOf()
}

actual suspend fun runLiteWork(call: () -> Unit) {}

@Composable
actual fun BoxScope.scrollBarIn(state: ScrollState) {
}

@Composable
actual fun demoUI(content: @Composable () -> Unit) {
    content()
}

actual class KFile(val path: Path) {
    actual val name: String get() = path.name

    actual suspend fun readText(): String = path.readText()
    actual suspend fun readLines(): List<String> = path.readLines()
    actual suspend fun writeText(text: String) = path.writeText(text)
    actual suspend fun writeLines(lines: List<String>) {
        path.writeLines(lines)
    }

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

}

lateinit var storageProvider: TextFileDocumentEmbeddingStorage<Path, Path>

//JVMDocumentProvider 内部支持java.nio.file.Path,导致
//filePath是保存向量文件的根目录
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

lateinit var chunkStorageProvider: FileDocumentEmbeddingStorage<FileChunk, Path>

actual suspend fun buildChunkStorage(path: String, callback: (List<String>) -> Unit) {
    val apiKey = getCacheString(DATA_APP_TOKEN)
    val embedder1 = buildEmbedder(apiKey!!)
    val embedder2 = TextDocumentEmbedder(JvmChunkDocumentProvider, embedder1)
//    val root = createRootDir("embed/index/chunk/${getFileLabel(path)}")
    val root = createRootDir("embed/index/chunk/rag")
    chunkStorageProvider = FileDocumentEmbeddingStorage<FileChunk, Path>(
        embedder = embedder2, documentProvider = JvmChunkDocumentProvider,
        fs = JVMFileSystemProvider.ReadWrite, root = Path(root)
    )
    val chunks = chunkFile(kotlinx.io.files.Path(path)) //段落切片

    val listId = mutableListOf<String>()
    chunks.forEachIndexed { index, chunk ->
        val id = chunkStorageProvider.store(chunk)
        listId.add(id)
    }
    callback(listId)
}

actual suspend fun searchRAGChunk(
    query: String,
    similarityThreshold: Double,
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
    return RagResult(query = query, evidence = list)
}