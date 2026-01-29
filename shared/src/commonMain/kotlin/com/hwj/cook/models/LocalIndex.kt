package com.hwj.cook.models

import kotlinx.serialization.Serializable

//整个向量化的记录表
@Serializable
data class LocalIndex(var indexedFiles: MutableList<IndexFile>? = null)

//每条向量化后的文件记录
@Serializable
data class IndexFile(
    val documentId: String?,
    val absolutePath: String?,
    val fileName: String?,
    val filePath: String?,//实际路径可能带特殊字符，进行处理
    val fileType: String?,
    val fileSize: Long?,
    val fileHash: String?
)