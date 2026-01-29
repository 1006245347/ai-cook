package com.hwj.cook.agent

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.rag.base.files.JVMDocumentProvider
import ai.koog.rag.base.files.JVMFileSystemProvider
import ai.koog.rag.vector.DocumentEmbedder
import ai.koog.rag.vector.FileDocumentEmbeddingStorage
import ai.koog.rag.vector.JVMTextFileDocumentEmbeddingStorage
import ai.koog.rag.vector.TextDocumentEmbedder
import ai.koog.rag.vector.TextFileDocumentEmbeddingStorage
import com.hwj.cook.KFile
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.writeLines


fun buildJvmStorage(embedder: LLMEmbedder): JVMTextFileDocumentEmbeddingStorage {
    val mDir = createRootDir("embed")
    val storage = JVMTextFileDocumentEmbeddingStorage(embedder, Path(mDir))
    return storage
}

//存单个文件
suspend fun storageFileEmbedding(sourceFile: String, storage: JVMTextFileDocumentEmbeddingStorage) {
    storage.store(Path(sourceFile)).also {
        printD("newEmbed>$it")
    }
}

//SystemFileSystem
suspend fun deleteEmbedFile() {
//    FileSystem.SYSTEM.createFile()

//    Path("").writeLines() //zhe ?
    val mFile = Path("ll.txt")
    val apiKey = getCacheString(DATA_APP_TOKEN)
    val embedder = buildEmbedder(apiKey!!)

    JVMFileSystemProvider.ReadWrite //关键是这个文件管理，iOS没有实现它没有Path去处理文档
    JVMDocumentProvider //文件处理提取

    //jvm常用
    val storage = TextFileDocumentEmbeddingStorage(
        embedder,
        JVMDocumentProvider,
        JVMFileSystemProvider.ReadWrite,
        mFile
    )
//    storage.store()
//    storage.allDocuments()
//    storage.delete()

    val documentEmbedder = TextDocumentEmbedder(JVMDocumentProvider, embedder)
    val storage2 = FileDocumentEmbeddingStorage(
        documentEmbedder, JVMDocumentProvider,
        JVMFileSystemProvider.ReadWrite, mFile
    )


}



