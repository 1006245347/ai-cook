package  com.hwj.cook

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalFileMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.Aes256GCMEncryptor
import ai.koog.agents.memory.storage.EncryptedStorage
import ai.koog.rag.base.files.JVMFileSystemProvider
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.agent.McpClientUtils
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.agent.tools.SwitchTools
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import com.hwj.cook.models.SuggestCookSwitch
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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.request
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.headers
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.zip.ZipInputStream
import kotlin.io.path.Path

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
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
//                    printD(message)
                    println(message)
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
    fun makeNode(f: File): BookNode {
        return if (f.isDirectory) {
            BookNode(
                name = f.name,
                isDirectory = true, realPath = f.absolutePath,
                loader = {
                    f.listFiles()?.map { makeNode(it) } ?: emptyList()
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

actual  suspend fun runLiteWork(call:()-> Unit){
//    McpClientUtils.searchMcpClientSSE("today is ?",getCacheString(DATA_MCP_KEY)!!)
//    McpClientUtils.tryClient()
    McpClientUtils.testMcp()
    call()
}