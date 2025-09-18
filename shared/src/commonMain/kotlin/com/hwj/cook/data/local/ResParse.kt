package com.hwj.cook.data.local

import com.hwj.cook.global.onlyDesktop
import com.hwj.cook.global.printD
import com.hwj.cook.listResourceFiles
import com.hwj.cook.models.BookNode

/**
 * @author by jason-何伟杰，2025/9/17
 * des:解析资源文件
 */
object ResParse {
    suspend fun loadRecipe(path: String = "files"): BookNode? {
//        val rootNode = listResourceFiles("files")
//        printD("size>${rootNode.name} ${rootNode.children.size}")

        if (onlyDesktop()) {

        }

        return null
    }


}