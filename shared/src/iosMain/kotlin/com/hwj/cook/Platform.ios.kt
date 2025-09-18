package com.hwj.cook

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.ai.global.askPermission
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import com.hwj.cook.models.BookNode
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.storage.STORAGE
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.serialization.json.Json
import platform.UIKit.UIDevice
import platform.Foundation.*

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val os: OsStatus
        get() = OsStatus.IOS
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun createKtorHttpClient(timeout: Long?): HttpClient {
    return HttpClient(Darwin) {
        install(HttpTimeout) {
            timeout?.let {
//                requestTimeoutMillis = it
                connectTimeoutMillis = timeout
                socketTimeoutMillis = 5000
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
    permission: PermissionPlatform,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    val p = when (permission) {
        PermissionPlatform.CAMERA -> Permission.CAMERA
        PermissionPlatform.GALLERY -> Permission.GALLERY
        PermissionPlatform.STORAGE -> Permission.STORAGE
        PermissionPlatform.WRITE_STORAGE -> Permission.WRITE_STORAGE
        else -> Permission.STORAGE
    }
    askPermission(p, grantedAction, deniedAction)
}

//判断目录是否是文件夹
@OptIn(ExperimentalForeignApi::class)
actual fun listResourceFiles(path: String): BookNode {
    val bundle = NSBundle.mainBundle
    val rootPath = bundle.resourcePath + "/$path"
    val fileManager = NSFileManager.defaultManager

    fun makeNode(dir: String, name: String): BookNode {
        return BookNode(
            name = name,
            isDirectory = true,
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
                        BookNode(name = file.toString(), isDirectory = false)
                    }
                }
            }
        )
    }

    return makeNode(rootPath, (path as NSString).lastPathComponent)
}

@OptIn(ExperimentalForeignApi::class)
actual fun readResourceFile(path: String): String {
    val bundle = NSBundle.mainBundle
    val filePath = bundle.pathForResource(path, null) ?: error("Resource not found: $path")
    return NSString.stringWithContentsOfFile(
        filePath,
        encoding = NSUTF8StringEncoding,
        error = null
    ) as String
}

@OptIn(ExperimentalForeignApi::class)
actual fun loadZipRes() {
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
}

@OptIn(ExperimentalForeignApi::class)
fun unzipResource(zipFilePath: String, targetDir: String) {

}


