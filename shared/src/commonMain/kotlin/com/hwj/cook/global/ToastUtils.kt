package com.hwj.cook.global

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ToastUtils {

private val _message = MutableStateFlow<String?>(null)

    val messageState= _message.asStateFlow()
    fun show(msg: String) {
        _message.value = msg
    }

    fun dismiss() {
        _message.value = null
    }
}