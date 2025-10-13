@file:OptIn(ExperimentalForeignApi::class)

package com.hwj.cook.agent

import ai.koog.rag.base.files.FileMetadata.FileContentType
import ai.koog.rag.base.files.FileMetadata.FileType
import ai.koog.rag.base.files.FileSystemProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path as KPath
import platform.Foundation.*
import platform.posix.*

/**
 * @author by jason-何伟杰，2025/10/13
 * des:参考 jvm的JVMFileSystemProvider
 */
class IOSFileSystemProvider {
    object ReadOnly : FileSystemProvider.ReadOnly<String> {

        override fun toAbsolutePathString(path: String): String {
            val full = if (path.startsWith("/")) path else NSFileManager.defaultManager.currentDirectoryPath + "/$path"
            return (full as NSString).stringByStandardizingPath
        }

        override fun fromAbsolutePathString(path: String): String = toAbsolutePathString(path)

        override fun joinPath(base: String, vararg parts: String): String {
            var result = base
            for (p in parts) {
                result = (result as NSString).stringByAppendingPathComponent(p)
            }
            return result
        }

        override fun name(path: String): String = (path as NSString).lastPathComponent
        override fun extension(path: String): String = (path as NSString).pathExtension

        override suspend fun metadata(path: String): FileMetadata? = withContext(Dispatchers.Default) {
            memScoped {
                val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null) ?: return@withContext null
                val type = attrs[NSFileType] as? String ?: return@withContext null
                val fileType = when (type) {
                    NSFileTypeDirectory -> FileType.Directory
                    NSFileTypeRegular -> FileType.File
                    else -> return@withContext null
                }
                FileMetadata(fileType, false)
            }
        }

        override suspend fun list(directory: String): List<String> = withContext(Dispatchers.Default) {
            val manager = NSFileManager.defaultManager
            val contents = manager.contentsOfDirectoryAtPath(directory, null) ?: return@withContext emptyList()
            contents.map { (directory as NSString).stringByAppendingPathComponent(it as String) }
        }

        override fun parent(path: String): String? {
            val parent = (path as NSString).stringByDeletingLastPathComponent
            return if (parent.isEmpty()) null else parent
        }

        override fun relativize(root: String, path: String): String? {
            return (path as NSString).substringFromIndex((root as NSString).length)
        }

        override suspend fun exists(path: String): Boolean = withContext(Dispatchers.Default) {
            memScoped {
                NSFileManager.defaultManager.fileExistsAtPath(path)
            }
        }

        override suspend fun getFileContentType(path: String): FileContentType = withContext(Dispatchers.Default) {
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
            val fileType = attrs?.get(NSFileType) as? String
            if (fileType == NSFileTypeRegular) FileContentType.Binary else FileContentType.Text
        }

        override suspend fun readBytes(path: String): ByteArray = withContext(Dispatchers.Default) {
            val data = NSData.dataWithContentsOfFile(path) ?: error("Cannot read file: $path")
            val bytes = ByteArray(data.length.toInt())
            memScoped {
                data.getBytes(bytes.refTo(0), data.length)
            }
            bytes
        }

        override suspend fun inputStream(path: String): Source = withContext(Dispatchers.Default) {
            SystemFileSystem.source(KPath(path)).buffered()
        }

        override suspend fun size(path: String): Long = withContext(Dispatchers.Default) {
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
            (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
        }
    }

    object ReadWrite : FileSystemProvider.ReadWrite<String>, FileSystemProvider.ReadOnly<String> by ReadOnly {

        override suspend fun create(path: String, type: FileType) = withContext(Dispatchers.Default) {
            val fm = NSFileManager.defaultManager
            when (type) {
                FileType.Directory -> fm.createDirectoryAtPath(path, true, null, null)
                FileType.File -> {
                    val parent = (path as NSString).stringByDeletingLastPathComponent
                    fm.createDirectoryAtPath(parent, true, null, null)
                    fm.createFileAtPath(path, null, null)
                }
            }
        }

        override suspend fun writeBytes(path: String, data: ByteArray): Unit = withContext(Dispatchers.Default) {
            val parent = (path as NSString).stringByDeletingLastPathComponent
            NSFileManager.defaultManager.createDirectoryAtPath(parent, true, null, null)
            val nsData = NSData.create(bytes = data.refTo(0), length = data.size.toULong())
            nsData.writeToFile(path, true)
        }

        override suspend fun outputStream(path: String, append: Boolean): Sink = withContext(Dispatchers.Default) {
            SystemFileSystem.sink(KPath(path), append = append).buffered()
        }

        override suspend fun move(source: String, target: String): Unit = withContext(Dispatchers.Default) {
            NSFileManager.defaultManager.moveItemAtPath(source, target, null)
        }

        override suspend fun copy(source: String, target: String): Unit = withContext(Dispatchers.Default) {
            NSFileManager.defaultManager.copyItemAtPath(source, target, null)
        }

        override suspend fun delete(path: String): Unit = withContext(Dispatchers.Default) {
            NSFileManager.defaultManager.removeItemAtPath(path, null)
        }
    }

}