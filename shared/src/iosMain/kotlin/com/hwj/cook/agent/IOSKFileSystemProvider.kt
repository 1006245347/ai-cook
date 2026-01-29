@file:OptIn(ExperimentalForeignApi::class)

package com.hwj.cook.agent

import ai.koog.rag.base.files.FileMetadata
import ai.koog.rag.base.files.FileSystemProvider
import com.hwj.cook.KFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSNumber
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.getBytes
import kotlinx.io.files.Path as KPath
import platform.Foundation.create
import platform.Foundation.writeToFile

public object IOSKFileSystemProvider {

    object ReadOnly : FileSystemProvider.ReadOnly<KFile> {

        override fun toAbsolutePathString(path: KFile): String =
            path.absolutePath

        override fun fromAbsolutePathString(path: String): KFile =
            KFile(path)

        override fun joinPath(base: KFile, vararg parts: String): KFile {
            var current = base
            for (p in parts) {
                current = current.resolve(p)
            }
            return current
        }

        override fun name(path: KFile): String = path.name

        override fun extension(path: KFile): String = path.extension

        override suspend fun metadata(path: KFile): FileMetadata? =
            withContext(Dispatchers.Default) {
                val attrs = NSFileManager.defaultManager
                    .attributesOfItemAtPath(path.filePath, null)
                    ?: return@withContext null

                val type = attrs[NSFileType] as? String ?: return@withContext null

                val fileType = when (type) {
                    NSFileTypeDirectory -> FileMetadata.FileType.Directory
                    NSFileTypeRegular -> FileMetadata.FileType.File
                    else -> return@withContext null
                }

                FileMetadata(fileType, false)
            }

        override suspend fun list(directory: KFile): List<KFile> =
            withContext(Dispatchers.Default) {
                val contents = NSFileManager.defaultManager
                    .contentsOfDirectoryAtPath(directory.filePath, null)
                    ?: return@withContext emptyList()

                contents.map {
                    directory.resolve(it as String)
                }
            }

        override fun parent(path: KFile): KFile? =
            path.parent()

        override fun relativize(root: KFile, path: KFile): String? {
            val rootPath = root.absolutePath
            val fullPath = path.absolutePath
            return if (fullPath.startsWith(rootPath)) {
                fullPath.removePrefix(rootPath).removePrefix("/")
            } else null
        }

        override suspend fun exists(path: KFile): Boolean =
            withContext(Dispatchers.Default) {
                NSFileManager.defaultManager.fileExistsAtPath(path.filePath)
            }

        override suspend fun getFileContentType(path: KFile): FileMetadata.FileContentType =
            withContext(Dispatchers.Default) {
                when (path.extension.lowercase()) {
                    "txt", "md", "json", "xml", "yaml", "yml", "kt", "java" ->
                        FileMetadata.FileContentType.Text

                    else ->
                        FileMetadata.FileContentType.Binary
                }
            }

        override suspend fun readBytes(path: KFile): ByteArray =
            withContext(Dispatchers.Default) {
                val data = NSData.dataWithContentsOfFile(path.filePath)
                    ?: error("Cannot read file: ${path.filePath}")

                val bytes = ByteArray(data.length.toInt())
                bytes.usePinned { pinned ->
                    data.getBytes(pinned.addressOf(0), data.length)
                }
                bytes
            }

        override suspend fun inputStream(path: KFile): Source =
            withContext(Dispatchers.Default) {
                SystemFileSystem
                    .source(KPath(path.filePath))
                    .buffered()
            }

        override suspend fun size(path: KFile): Long =
            withContext(Dispatchers.Default) {
                val attrs = NSFileManager.defaultManager
                    .attributesOfItemAtPath(path.filePath, null)
                (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
            }

    }

    object ReadWrite :
        FileSystemProvider.ReadWrite<KFile>,
        FileSystemProvider.ReadOnly<KFile> by ReadOnly {

        override suspend fun create(path: KFile, type: FileMetadata.FileType): Unit =
            withContext(Dispatchers.Default) {
                val fm = NSFileManager.defaultManager
                when (type) {
                    FileMetadata.FileType.Directory ->
                        fm.createDirectoryAtPath(
                            path.filePath,
                            true,
                            null,
                            null
                        )

                    FileMetadata.FileType.File -> {
                        val parent = path.parent()
                        if (parent != null) {
                            fm.createDirectoryAtPath(
                                parent.filePath,
                                true,
                                null,
                                null
                            )
                        }
                        fm.createFileAtPath(path.filePath, null, null)
                    }
                }
                Unit
            }

        override suspend fun writeBytes(path: KFile, data: ByteArray): Unit =
            withContext(Dispatchers.Default) {
                val parent = path.parent()
                if (parent != null) {
                    NSFileManager.defaultManager.createDirectoryAtPath(
                        parent.filePath,
                        true,
                        null,
                        null
                    )
                }

                data.usePinned { pinned ->
                    val nsData = NSData.create(
                        bytes = pinned.addressOf(0),
                        length = data.size.toULong()
                    )
                    nsData.writeToFile(path.filePath, true)
                }
            }

        override suspend fun outputStream(path: KFile, append: Boolean): Sink =
            withContext(Dispatchers.Default) {
                SystemFileSystem
                    .sink(KPath(path.filePath), append = append)
                    .buffered()
            }

        override suspend fun move(source: KFile, target: KFile): Unit =
            withContext(Dispatchers.Default) {
                NSFileManager.defaultManager.moveItemAtPath(
                    source.filePath,
                    target.filePath,
                    null
                )
            }

        override suspend fun copy(source: KFile, target: KFile): Unit =
            withContext(Dispatchers.Default) {
                NSFileManager.defaultManager.copyItemAtPath(
                    source.filePath,
                    target.filePath,
                    null
                )
            }

        override suspend fun delete(path: KFile): Unit =
            withContext(Dispatchers.Default) {
                NSFileManager.defaultManager.removeItemAtPath(
                    path.filePath,
                    null
                )
            }
    }
}