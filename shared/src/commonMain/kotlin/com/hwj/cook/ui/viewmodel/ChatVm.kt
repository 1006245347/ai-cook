package com.hwj.cook.ui.viewmodel

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.isRunning
import ai.koog.utils.io.use
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.agent.provider.AICookAgentProvider
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.models.AgentUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

/**
 * @author by jason-何伟杰，2025/10/9
 * des:智能体聊天
 */
class ChatVm(
    private val globalRepository: GlobalRepository
) : ViewModel() {
    private var agentProvider: AICookAgentProvider? = null
    private var agentInstance: AIAgent<String, String>?=null
    private val _uiState = MutableStateFlow(
        AgentUiState(
            title = agentProvider?.title,
            messages = listOf(ChatMsg.SystemMsg(agentProvider?.description))
        )
    )

    val uiObs: StateFlow<AgentUiState> = _uiState.asStateFlow()

    var curChatJob: kotlinx.coroutines.Job? = null
    //停止接收回答
    private val _stopReceivingObs = MutableStateFlow(false)
    val stopReceivingState = _stopReceivingObs.asStateFlow()

    fun createAgent(isForce: Boolean = false) {
        if (agentProvider == null || isForce) {
            agentProvider = AICookAgentProvider()
        }
    }

    fun updateInputText(txt: String) {
        _uiState.update { it.copy(inputTxt = txt) }
    }

    fun sendMessage() {
        val userInput = _uiState.value.inputTxt.trim()
        if (userInput.isEmpty()) return

        if (_uiState.value.userResponseRequested) { //回复智能体的问题，用户再输入
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMsg.UserMsg(userInput),
                    inputTxt = "",
                    isLoading = true,
                    userResponseRequested = false, currentUserResponse = userInput
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMsg.UserMsg(userInput),
                    inputTxt = "", isInputEnabled = false, isLoading = true
                )
            }
        }

        viewModelScope.launch(Dispatchers.Default) { runAgent(userInput) }
    }

    private suspend fun runAgent(userInput: String) {
        try {
            agentInstance = agentProvider?.provideAgent(onToolCallEvent = { msg ->
                viewModelScope.launch {
                    _uiState.update { it.copy(messages = it.messages + ChatMsg.ToolCallMsg(msg)) }
                }
            }, onErrorEvent = { errorMsg ->
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMsg.ErrorMsg(errorMsg),
                            isInputEnabled = true,
                            isLoading = false
                        )
                    }
                }
            }, onAssistantMessage = { msg ->
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMsg.AgentMsg(msg),
                        isInputEnabled = true, isLoading = false, userResponseRequested = true
                    )
                }
                // Wait for user response
                val userResponse = _uiState
                    .first { it.currentUserResponse != null }
                    .currentUserResponse
                    ?: throw IllegalArgumentException("User response is null")

                // Update the state to reset current response
                _uiState.update {
                    it.copy(
                        currentUserResponse = null
                    )
                }

                // Return it to the agent
                userResponse
            })

            agentInstance?.use { t ->
                val result = t.run(userInput)

                _uiState.update {
                    it.copy(
                        messages = it.messages +
                                ChatMsg.ResultMsg(result),// + ChatMsg.SystemMsg("The agent has stopped."),
                        isInputEnabled = false,
                        isLoading = false,
                        isChatEnded = true
                    )
                }
//                "Agent done with $result"//有什么用？
            }

        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMsg.ErrorMsg("error:${e.message}"),
                    isInputEnabled = true, isLoading = false
                )
            }
        }
    }

    fun restartRun() {
        _uiState.update {
            AgentUiState(
                title = agentProvider?.title,
                messages = listOf(ChatMsg.SystemMsg(agentProvider?.description))
            )
        }
    }

    fun createNewChat(){
        agentProvider?.let {

        }
    }

    fun saveMsg(){

    }
}