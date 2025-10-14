package com.hwj.cook

import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.agents.memory.providers.LocalMemoryConfig
import ai.koog.agents.memory.storage.SimpleStorage
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.global.askPermission
import com.hwj.cook.agent.IOSFileMemoryProvider
import com.hwj.cook.agent.IOSFileSystemProvider
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
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
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
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
import platform.Foundation.lastPathComponent
import platform.Foundation.stringWithContentsOfFile
import platform.UIKit.UIDevice

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
    vararg permissions: Any,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    val p = when (permission) {
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

    private fun getCpuArchitecture(): String? {
        memScoped {
            val size = alloc<size_tVar>()
            sysctlbyname("hw.machine", null, size.ptr, null, 0)
            val buffer = allocArray<ByteVar>(size.value.toInt())
            sysctlbyname("hw.machine", buffer, size.ptr, null, 0)
            return buffer.toKString()
        }
    }
    val device = UIDevice.currentDevice
    val processInfo = NSProcessInfo.processInfo

    val cpuCores = processInfo.processorCount.toInt()
    val cpuArch = getCpuArchitecture()
    val totalMemoryMB = processInfo.physicalMemory / (1024 * 1024)

    return DeviceInfoCell(
        cpuCores = cpuCores,
        cpuArch = cpuArch,
        totalMemoryMB = totalMemoryMB,
        brand = "Apple",
        model = device.model,
        osVersion = "${device.systemName} ${device.systemVersion}",
        platform = "iOS"
    )
}