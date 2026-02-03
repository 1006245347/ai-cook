package com.hwj.cook.agent

import ai.koog.rag.base.files.DocumentProvider
import com.hwj.cook.agent.rag.FileChunk
import com.hwj.cook.global.printD
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import java.nio.file.Path

/**
 * @author by jason-何伟杰，2026/2/3
 * des:Path是文件的路径，我们的切片都是同一个文件切来的，这没法对应上。。
 */
object JvmChunkDocumentProvider : DocumentProvider<Path, FileChunk> {
    override suspend fun document(path: Path): FileChunk? {
//        error("chunk 自己生成 $path")
        // /Users/jasonmac/Library/Application Support/AI_COOK/embed/index/chunk/rag/documents/17e6ac97-d7c2-463a-a371-5828d2dc2648
        //不可以这样，koog是加载所有的知识库文件，每个都读出来？我擦
        return FileChunk(
            path = kotlinx.io.files.Path(path.toString()),
            index = 0,
            text = PlatformFile(path.toString()).readString(),
            start = -1,
            end = -1
        )
    }

    override suspend fun text(document: FileChunk): CharSequence {
        return document.text
    }
}