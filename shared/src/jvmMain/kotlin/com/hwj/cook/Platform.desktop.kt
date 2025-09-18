package  com.hwj.cook

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import com.hwj.cook.global.printLog
import com.hwj.cook.models.BookNode
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

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
    permission: PermissionPlatform,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    grantedAction()
}

actual fun createKtorHttpClient(timeout: Long?): HttpClient {
    return HttpClient {
        install(HttpTimeout) {
            timeout?.let {
//                requestTimeoutMillis = it //完整请求超时
                connectTimeoutMillis = timeout //建立链接
                socketTimeoutMillis = 5000
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true

            })
        }
//        install(SSE)
        install(Logging) {
//                level = LogLevel.ALL
//            level = LogLevel.INFO //接口日志屏蔽
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    }
}

actual fun listResourceFiles(path: String): BookNode {
    val url = object {}.javaClass.getResource("/$path")
        ?: error("Resource not found: $path")
    printLog("url>$url")
    val rootFile = File(url.toURI())

    fun makeNode(f: File): BookNode {
        return if (f.isDirectory) {
            BookNode(
                name = f.name,
                isDirectory = true,
                loader = {
                    f.listFiles()?.map { makeNode(it) } ?: emptyList()
                }
            )
        } else {
            BookNode(name = f.name, isDirectory = false)
        }
    }

    return makeNode(rootFile)
}

actual fun readResourceFile(path: String): String {
    val stream = object {}.javaClass.getResourceAsStream("/$path")
        ?: error("Resource not found: $path")
    return stream.bufferedReader().use { it.readText() }
}

actual fun loadZipRes() {
    val folder = System.getProperty("user.home" )+ "/.aicook/files"
    val target = File(folder).also { printLog(it.absolutePath) }
    if (!target.exists()) target.mkdirs()
    val zipStream =
        object {}.javaClass.getResourceAsStream("/resource.zip") ?: error("not found hwj")

    unzipResource(zipStream, target.absolutePath)
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
