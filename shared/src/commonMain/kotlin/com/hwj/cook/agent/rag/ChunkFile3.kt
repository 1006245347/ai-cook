package com.hwj.cook.agent.rag

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import kotlinx.io.files.Path

suspend fun chunkFile(
    path: Path,
    maxChars: Int = 300,
    overlap: Int = 100
): List<FileChunk> {
    val text = PlatformFile(path).readString()
    return paragraphChunking(text, path, maxChars, overlap)
//    return paragraphChunking2(text, path, maxChars, overlap)
}

data class FileChunk(
    val path: Path,
    val index: Int,
    val text: String,
    val start: Int,
    val end: Int
)

private fun paragraphChunking(
    text: String,
    path: Path,
    maxChars: Int,
    overlap: Int
): List<FileChunk> {

    val paragraphs = text
        .split(Regex("\n{2,}"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val chunks = mutableListOf<FileChunk>()
    val buffer = StringBuilder()

    var chunkIndex = 0
    var hasNewContentSinceLastFlush = false

    fun flushChunk(force: Boolean = false) {
        val content = buffer.toString().trim()
        if (content.isEmpty()) return

        // 🔴 核心修复点：禁止“纯 overlap 生成 chunk”
        if (!force && !hasNewContentSinceLastFlush) return
//        if (!force&& content.length<=overlap)return

        chunks += FileChunk(
            path = path,
            index = chunkIndex++,
            text = content,
            start = -1, // 如果你需要精确 offset，建议单独计算
            end = -1
        )

        val overlapText = extractSafeOverlap(content, overlap)
//        val overlapText =if (content.length>overlap) content.takeLast(overlap) else content
        buffer.clear()
        buffer.append(overlapText)
        hasNewContentSinceLastFlush = false
    }

    for (para in paragraphs) {
        val sentences = splitBySentence(para)

        for (sentence in sentences) {

            if (sentence.length > maxChars) {
                flushChunk(force = true)

                hardCutSentence(sentence, maxChars).forEach {
                    chunks += FileChunk(
                        path = path,
                        index = chunkIndex++,
                        text = it,
                        start = -1,
                        end = -1
                    )
                }
                buffer.clear()
                hasNewContentSinceLastFlush = false
                continue
            }

            if (buffer.length + sentence.length > maxChars) {
                flushChunk(force = true)
            }

            buffer.append(sentence)
            hasNewContentSinceLastFlush = true
        }

        flushChunk()
    }

    flushChunk(force = true)
    return chunks
}

private fun paragraphChunking2(
    text: String,
    path: Path,
    maxChars: Int,
    overlap: Int
): List<FileChunk> {

    val paragraphs = text
        .split(Regex("\n{2,}"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val chunks = mutableListOf<FileChunk>()
    val buffer = StringBuilder()
    var chunkIndex = 0

    fun flushChunk() {
        val content = buffer.toString().trim()
        if (content.isEmpty()) return

        // 如果 chunk 内容 <= overlap，说明没有新增内容，跳过
        if (chunks.isNotEmpty() && content.length <= overlap) return

        chunks += FileChunk(
            path = path,
            index = chunkIndex++,
            text = content,
            start = -1,
            end = -1
        )

        // 只保留最后 overlap 作为下一次 buffer 的开头
        buffer.clear()
        if (content.length > overlap) {
            buffer.append(content.takeLast(overlap))
        }
    }

    for (para in paragraphs) {
        val sentences = splitBySentence(para)

        for (sentence in sentences) {

            if (sentence.length > maxChars) {
                flushChunk()
                hardCutSentence(sentence, maxChars).forEach {
                    chunks += FileChunk(
                        path = path,
                        index = chunkIndex++,
                        text = it,
                        start = -1,
                        end = -1
                    )
                }
                buffer.clear()
                continue
            }

            // 如果 buffer 加上 sentence 超过 maxChars，先 flush buffer
            if (buffer.length + sentence.length > maxChars) {
                flushChunk()
            }

            buffer.append(sentence)
        }

        flushChunk()
    }

    flushChunk()
    return chunks
}

private fun extractSafeOverlap(text: String, maxOverlap: Int): String {
    if (text.length <= maxOverlap) return text

    val start = text.length - maxOverlap
    val sub = text.substring(start)

    // 优先找句子边界
    val sentenceIdx = Regex("[。！？.!?]\\s").findAll(sub).lastOrNull()
    if (sentenceIdx != null) {
        return sub.substring(sentenceIdx.range.last + 1)
    }

    // 次优：找空格（英文单词）
    val spaceIdx = sub.indexOf(' ')
    if (spaceIdx != -1) {
        return sub.substring(spaceIdx + 1)
    }

    return sub // 兜底
}

private fun splitBySentence(text: String): List<String> {
    val regex = Regex(
        "(?<=[。！？.!?])\\s+|\n"
    )
    return text
        .split(regex)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { "$it " } // 保留阅读节奏
}

private fun hardCutSentence(
    sentence: String,
    maxChars: Int
): List<String> {
    val result = mutableListOf<String>()
    var start = 0
    while (start < sentence.length) {
        val end = minOf(start + maxChars, sentence.length)
        result += sentence.substring(start, end)
        start = end
    }
    return result
}