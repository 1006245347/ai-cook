package com.hwj.cook.global

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * @author by jason-何伟杰，2025/4/25
 * des:国际化怎么做呢，切换时重新渲染
 */
object StrUtils {
    private val _currentLocale = mutableStateOf(true)

    val currentLocale: State<Boolean> = _currentLocale

    fun switchTo(isChinese: Boolean) {
        _currentLocale.value = isChinese
    }

    val switchLanguage: String
        get() = if (currentLocale.value) "用中文回答" else "Answer in chinese"
}

//用事件透传 把任务放到协程进行
suspend fun detectLanguage() {

}