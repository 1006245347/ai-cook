package com.hwj.cook.ui.viewmodel

import androidx.compose.runtime.snapshotFlow
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.except.NetworkObserver
import com.hwj.cook.global.CODE_IS_DARK
import com.hwj.cook.global.getCacheBoolean
import com.hwj.cook.models.AppIntent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

/**
 * @author by jason-何伟杰，2025/9/18
 * des:记得依赖注入
 */
class MainVm(private val globalRepo: GlobalRepository, networkObserver: NetworkObserver) :
    ViewModel() {

    private val _isNetObs = MutableStateFlow(false)
    val isNetState = _isNetObs.asStateFlow()

    //本地主题值修改
    private val _darkObs = MutableStateFlow(false)
    val darkState = _darkObs.asStateFlow()

//    //首页是否折叠抽屉
//    private val _isCollapsedObs = MutableStateFlow(false)
//    val isCollapsedState = _isCollapsedObs.asStateFlow()

    suspend fun checkNetStatus() {
        _isNetObs.value = globalRepo.isConnectNet()
    }

    fun initialize() {
        viewModelScope.launch {
            delay(2000)
            createRootDir()
        }
    }

    //val isLinked by viewModel.isLinked.collectAsStateWithLifecycle()
    val isLinked = combine(
        networkObserver.observe(), snapshotFlow { _darkObs.value }
    ) { status, isDark ->
        status == NetworkObserver.Status.Connected && isDark
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)//一创建就立即开始收集上游 Flow,初始false


    //事件处理 执行意图所对应的事件
    fun processIntent(intent: AppIntent, thread: CoroutineDispatcher = Dispatchers.Default) {
        viewModelScope.launch(thread) {
            when (intent) {
                is AppIntent.ThemeSetIntent -> {
                    _darkObs.value = getCacheBoolean(CODE_IS_DARK)
                }
                else ->{}
            }
        }
    }


    //UI处理
    fun collapsedDrawer() {
//        _isCollapsedObs.value = !isCollapsedState.value

    }

    //数据处理

}