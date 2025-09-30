package com.hwj.cook.data.local

import com.hwj.cook.global.DATA_BOOK_ROOT
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printLog
import com.hwj.cook.global.removeCacheKey
import com.hwj.cook.global.saveString
import com.hwj.cook.listResourceFiles
import com.hwj.cook.loadZipRes
import com.hwj.cook.models.BookNode

/**
 * @author by jason-何伟杰，2025/9/17
 * des:解析资源文件
 */
object ResParse {

    //先把应用内资源文件解压到设备指定目录，再构建树结构的菜谱目录
    suspend fun loadRecipe(): BookNode? {
//        removeCacheKey(DATA_BOOK_ROOT) //test
        var rootNode: BookNode? = null
        val tmpRoot = getCacheString(DATA_BOOK_ROOT).also { printLog("tmpRoot>$it") }

        if (tmpRoot != null) {
            listResourceFiles(tmpRoot).also { bookNode ->
                rootNode = bookNode
            }
        } else {
            loadZipRes().also { rootPath ->
                rootPath?.let {
                    listResourceFiles(rootPath)?.also { bookNode ->
                        if (!bookNode.children.isEmpty()) {
                            saveString(DATA_BOOK_ROOT, rootPath)
                        }
                        rootNode = bookNode
                    }
                }
            }
        }
        return rootNode
    }
}