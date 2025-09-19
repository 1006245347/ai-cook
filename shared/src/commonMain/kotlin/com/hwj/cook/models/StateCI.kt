package com.hwj.cook.models

/**
 * @author by jason-何伟杰，2025/9/18
 * des: 把所有状态声明放这里
 */
data class AppConfigState(
    val isLoading: Boolean = false,
    val data: BookNode? = null,
    val error: String? = null
)

//AVI 意图区别，项目小，为了方便只分两大类UI、Data.
sealed class UiIntent {
    //明亮、黑暗主题切换
    data object ThemeSetIntent : UiIntent()
}

sealed class DataIntent {
    data object CookLoadIntent : DataIntent()
}