package com.hwj.cook.agent

import ai.koog.rag.base.files.DocumentProvider
import com.hwj.cook.KFile

/**
 * @author by jason-何伟杰，2026/1/29
 * des: 类似JVMDocumentProvider，其实就是iOS不支持，所以再搞个通用的
 */
public object GlobalDocumentProvider : DocumentProvider<KFile, KFile> {

    override suspend fun document(path: KFile): KFile {
        return path
    }

    override suspend fun text(document: KFile): CharSequence {
        return convertLineSeparators(document.readText())
    }


    object Edit : DocumentProvider.Edit<KFile, KFile> {
        override suspend fun setText(
            document: KFile,
            text: String,
            range: DocumentProvider.DocumentRange?
        ) {

            if (range != null) {
                val fileLines = document.readLines()
                document.writeLines(modifyLines(fileLines, text, range))
            } else {
                document.writeText(text)
            }
        }

    }
}


/**
 * 替换文本行中指定范围的内容
 */
private fun modifyLines(
    lines: List<String>,
    newText: String,
    range: DocumentProvider.DocumentRange
): List<String> {
    require(range.start <= range.end) { "Start position is greater than end position" }
    require(range.start.line in lines.indices) { "Start line ${range.start.line} is out of file lines range" }
    require(range.start.column <= lines[range.start.line].length) {
        "Start column ${range.start.column} is not in line ${range.start.line} range"
    }
    require(range.end.line in lines.indices) { "End line ${range.end.line} is out of file lines range" }
    require(range.end.column <= lines[range.end.line].length) {
        "End column ${range.end.column} is not in line ${range.end.line} range"
    }

    val startLine = lines[range.start.line].substring(0, range.start.column)
    val endLine = lines[range.end.line].substring(range.end.column)

    val newLines = mutableListOf<String>()
    val oldLinesIterator = lines.listIterator()

    // 保留 range.start.line 之前的内容
    for (i in 0 until range.start.line) {
        newLines.add(oldLinesIterator.next())
    }

    // 替换行
    newLines.addAll((startLine + newText + endLine).lineSequence())

    // 跳过已替换的区间
    for (i in range.start.line..range.end.line) {
        oldLinesIterator.next()
    }

    // 追加剩余的行
    newLines.addAll(oldLinesIterator.asSequence())

    return newLines
}

/**
 * 统一换行符为 `\n`
 */
private fun convertLineSeparators(text: String): String {
    val buffer = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\r') {
            val followedByLineFeed = i < text.length - 1 && text[i + 1] == '\n'
            buffer.append('\n')
            if (followedByLineFeed) i++ // 跳过 \r\n 中的 \n
        } else {
            buffer.append(c)
        }
        i++
    }
    return buffer.toString()
}