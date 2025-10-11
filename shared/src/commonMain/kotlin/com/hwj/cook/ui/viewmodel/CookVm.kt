package com.hwj.cook.ui.viewmodel

import com.hwj.cook.data.local.ResParse
import com.hwj.cook.global.getMills
import com.hwj.cook.global.printLog
import com.hwj.cook.models.AppIntent
import com.hwj.cook.models.BookConfigState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

/**
 * @author by jason-何伟杰，2025/9/28
 * des:菜谱处理
 */
class CookVm : ViewModel() {

    private val _bookRootObs = MutableStateFlow(BookConfigState())
    val bookRootState = _bookRootObs.asStateFlow()
    private var lastTime = 0L

    fun initialize() {
        printLog("doInit?")
        processIntent(AppIntent.BookLoadIntent)
    }

    fun loadBookRootData() {
        _bookRootObs.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = ResParse.loadRecipe()
                lastTime=getMills()
                _bookRootObs.update { it.copy(isLoading = false, data = root) }
            } catch (e: Exception) {
                _bookRootObs.update { it.copy(error = e.toString()) }
            }
        }
    }

    fun processIntent(intent: AppIntent, thread: CoroutineDispatcher = Dispatchers.Default) {
        viewModelScope.launch(thread) {
            when (intent) {
                is AppIntent.BookLoadIntent -> {
                    if (getMills() - lastTime > 10 * 60 * 1000) {
                    }
                        loadBookRootData()
                }

                else -> {}
            }
        }
    }
}