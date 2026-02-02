package com.hwj.cook

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.SimpleStorage
import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.rag.vector.TextFileDocumentEmbeddingStorage
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.agent.GlobalDocumentProvider
import com.hwj.cook.agent.IOSFileMemoryProvider
import com.hwj.cook.agent.IOSFileSystemProvider
import com.hwj.cook.agent.IOSKFileSystemProvider
import com.hwj.cook.agent.buildEmbedder
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import com.hwj.cook.global.askPermission
import com.hwj.cook.global.getCacheString
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.storage.STORAGE
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HeadersBuilder
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.stringByDeletingLastPathComponent
import platform.Foundation.stringByStandardizingPath
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val os: OsStatus
        get() = OsStatus.IOS
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun createKtorHttpClient(timeout: Long?, builder: HeadersBuilder.() -> Unit): HttpClient {
    return HttpClient(Darwin) {
        defaultRequest {
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
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
//                level = LogLevel.ALL
            level = LogLevel.NONE //接口日志屏蔽
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    }
}

actual fun checkSystem(): OsStatus {
    return OsStatus.IOS
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
    val p = when (permissions[0] as PermissionPlatform) {
        PermissionPlatform.CAMERA -> Permission.CAMERA
        PermissionPlatform.GALLERY -> Permission.GALLERY
        PermissionPlatform.STORAGE -> Permission.STORAGE
        else -> Permission.STORAGE
    }
    askPermission(p, grantedAction, deniedAction)
}

@OptIn(ExperimentalForeignApi::class)
actual fun loadZipRes(): String? {
    val dir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask, true
    ).first() as String
    val folder = "$dir/files"
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(folder)) {
        fm.createDirectoryAtPath(folder, true, null, null)
    }

    val resPath = NSBundle.mainBundle.pathForResource("resource", "zip") ?: error("not hwj")
    unzipResource(resPath, folder)
    return folder
}

fun unzipResource(zipFilePath: String, targetDir: String) {
    //实现ios压缩功能
}

//判断目录是否是文件夹
@OptIn(ExperimentalForeignApi::class)
actual fun listResourceFiles(path: String): BookNode? {
    val bundle = NSBundle.mainBundle
    val rootPath = bundle.resourcePath + "/$path"
    val fileManager = NSFileManager.defaultManager

    fun makeNode(dir: String, name: String): BookNode {
        return BookNode(
            name = name,
            isDirectory = true, realPath = dir,
            loader = {
                val files =
                    fileManager.contentsOfDirectoryAtPath(dir, null) as? List<Any?> ?: emptyList()
                files.map { file ->
                    val childPath = "$dir/$file"
                    // 用 attributesOfItemAtPath 来判断文件/目录
                    val attrs = fileManager.attributesOfItemAtPath(childPath, null)
                    val fileType = attrs?.get(NSFileType) as? String
                    val isDir = fileType == NSFileTypeDirectory

                    if (isDir) {
                        makeNode(childPath, file.toString())
                    } else {
                        BookNode(name = file.toString(), isDirectory = false, realPath = childPath)
                    }
                }
            }
        )
    }

    return makeNode(rootPath, (path as NSString).lastPathComponent)
}

@OptIn(ExperimentalForeignApi::class)
actual fun readResourceFile(path: String): String? {
    val bundle = NSBundle.mainBundle
    val filePath = bundle.pathForResource(path, null) ?: error("Resource not found: $path")
    return NSString.stringWithContentsOfFile(
        filePath,
        encoding = NSUTF8StringEncoding,
        error = null
    ) as String
}

actual fun createFileMemoryProvider(scopeId: String): AgentMemoryProvider {
    return IOSFileMemoryProvider(
        config = LocalMemoryConfig("memory-cache/$scopeId"),
        storage = SimpleStorage(IOSFileSystemProvider.ReadWrite),
        fs = IOSFileSystemProvider.ReadWrite,
        root = createRootDir()
    )
}

actual fun getDeviceInfo(): DeviceInfoCell {

    return DeviceInfoCell(
        cpuCores = 1,//cpuCores,
        cpuArch = "1",//cpuArch,
        totalMemoryMB = 1, //totalMemoryMB,
        brand = "Apple",
        model = "1", //device.model,
        osVersion = "1", //"${device.systemName} ${device.systemVersion}",
        platform = "iOS"
    )
}

actual interface PlatformToolSet
actual interface KmpToolSet : PlatformToolSet

actual fun platformAgentTools(): ToolRegistry {
    return ToolRegistry { }
}

actual fun plusAgentList(): List<AgentInfoCell> {
    return listOf()
}

actual suspend fun runLiteWork(call: () -> Unit) {
    call()
}

@Composable
actual fun demoUI(content: @Composable () -> Unit) {
    content()
}

@Composable
actual fun BoxScope.scrollBarIn(state: ScrollState) {
}


@OptIn(BetaInteropApi::class)
actual class KFile(val filePath: String) {

    actual val name: String get() = (NSString.create(filePath)).lastPathComponent

    actual val extension: String
        get() = (NSString.create(filePath)).pathExtension

    actual val absolutePath: String
        get() = (NSString.create(filePath)).stringByStandardizingPath

    actual fun resolve(child: String): KFile =
        KFile((NSString.create(filePath)).stringByAppendingPathComponent(child))

    actual fun parent(): KFile? {
        val p = (NSString.create(filePath)).stringByDeletingLastPathComponent
        return if (p.isEmpty()) null else KFile(p)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun readText(): String =
        NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null) as String

    actual suspend fun readLines(): List<String> =
        readText().split("\n")

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun writeText(text: String) {

        val str = NSString.create(text)
        str.writeToFile(filePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual suspend fun writeLines(lines: List<String>) {
        writeText(lines.joinToString("\n"))
    }
}

lateinit var storageProvider: TextFileDocumentEmbeddingStorage<KFile, KFile>

actual suspend fun buildFileStorage(filePath: String, embedder: LLMEmbedder) {
    val mFile = KFile(filePath)
    val apiKey = getCacheString(DATA_APP_TOKEN)
    val embedder = buildEmbedder(apiKey!!)
    val storage = TextFileDocumentEmbeddingStorage(
        embedder,
        GlobalDocumentProvider,
        IOSKFileSystemProvider.ReadWrite,
        mFile
    )
    storageProvider = storage
}

actual suspend fun storeFile(filePath: String, callback: (String?) -> Unit) {
    val id = storageProvider.store(KFile(filePath))
    callback(id)
}

actual suspend fun deleteRAGFile(documentId: String) {
    storageProvider.delete(documentId = documentId)
}


