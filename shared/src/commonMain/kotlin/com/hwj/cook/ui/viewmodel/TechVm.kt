package com.hwj.cook.ui.viewmodel

import com.hwj.cook.agent.provider.MemoryAgentProvider
import com.hwj.cook.global.DATA_MEMORY_INPUT
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printLog
import com.hwj.cook.global.saveString
import com.hwj.cook.models.MemoryUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

/**
 * @author by jason-何伟杰，2025/10/13
 * des:agent存事实、某类主题、应用范围的记忆
 */
class TechVm : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiObs: StateFlow<MemoryUiState> = _uiState.asStateFlow()

    private var agentProvider: MemoryAgentProvider? = null

    fun updateInputText(txt: String) {
        _uiState.update { it.copy(inputTxt = txt) }
    }

    suspend fun loadInputCache() {
        printLog("loadInputCache??")
        //把输入的内容存下来，大模型的记忆不对外显示
        getCacheString(DATA_MEMORY_INPUT, "")?.let {
            updateInputText(it)
        }
    }

    fun sendFact2Memory() {
        val userInput = _uiState.value.inputTxt.trim()
        if (userInput.isEmpty()) return

        _uiState.update { it.copy(inputTxt = "", isInputEnded = false, isLoading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            runAgent(userInput)
        }
    }

    private suspend fun runAgent(userInput: String) {
        try {
            val agent = agentProvider?.provideAgent({}, onErrorEvent = { errorMsg ->
                _uiState.update { it.copy(isInputEnded = true, isLoading = false) }
            }, { "" })
            val result = agent?.run(userInput)
            _uiState.update {
                it.copy(
                    isInputEnabled = false,
                    isLoading = false,
                    isInputEnded = true,
                    memoryOfUser = result
                )
            }
            saveString(DATA_MEMORY_INPUT,userInput)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(isInputEnded = true, isLoading = false)
            }
        }
    }

    fun restartRun() {
        _uiState.update { MemoryUiState() }
    }

    fun createAgent(isForce: Boolean = false) {
        if (agentProvider == null || isForce) {
            agentProvider = MemoryAgentProvider()
        }
    }
}