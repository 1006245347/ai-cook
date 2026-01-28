package com.hwj.cook.models

import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2026/1/28
 * des: RAG处理的文件结构
 */
@Serializable
data class FileInfoCell(
    val path: String,
    val name: String,
    val millDate: Long,
    val fileSize: Long,
    val isEmbed: Boolean
)