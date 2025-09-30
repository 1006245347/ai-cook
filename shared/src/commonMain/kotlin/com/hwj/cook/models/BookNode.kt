package com.hwj.cook.models

import kotlinx.serialization.Serializable


//懒加载父目录树节点
class BookNode(
    val name: String, val isDirectory: Boolean,val realPath :String,
    private val loader: (() -> List<BookNode>)? = null
) {
    val children: List<BookNode> by lazy { loader?.invoke() ?: emptyList() }
}
