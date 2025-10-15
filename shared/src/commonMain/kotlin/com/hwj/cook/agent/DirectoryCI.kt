package com.hwj.cook.agent

import com.hwj.cook.global.printLog
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.projectDir

/**
 * @author by jason-何伟杰，2025/10/11
 * des:本项目所有关于平台文件夹处理汇总
 */

fun projectDir(): String {
    return FileKit.projectDir.absolutePath()
}

fun fileDir(): String {
    return FileKit.filesDir.absolutePath()
}

fun cacheDir(): String {
    return FileKit.cacheDir.absolutePath()
}

//注意权限申请
fun createRootDir(subDir: String = "ai"): String { //  /. /data/user/0/com.hwj.cook.android/files /data/user/0/com.hwj.cook.android/cache
//    printLog("root>${projectDir()} ${fileDir()} ${cacheDir()} ")
    return PlatformFile(fileDir() + "/$subDir").apply {
        createDirectories()
    }.absolutePath().also {
        printLog(it)
    }
}