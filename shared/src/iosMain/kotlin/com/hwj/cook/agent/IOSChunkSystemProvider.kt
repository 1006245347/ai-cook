package com.hwj.cook.agent

import ai.koog.rag.base.files.DocumentProvider
import com.hwj.cook.KFile
import com.hwj.cook.agent.rag.FileChunk
import kotlinx.io.files.Path

object IOSChunkSystemProvider : DocumentProvider<KFile, FileChunk> {
    override suspend fun document(path: KFile): FileChunk? {

        return FileChunk(
            path = Path(path.absolutePath),
            index = 0,
            text = path.absolutePath, //正常是内容，检索时内容太多不好做映射处理即可
            start = -1, end = -1
        )
    }

    override suspend fun text(document: FileChunk): CharSequence {
        return document.text
    }

}