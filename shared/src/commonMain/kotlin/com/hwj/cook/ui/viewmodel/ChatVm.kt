@file:OptIn(ExperimentalUuidApi::class)

package com.hwj.cook.ui.viewmodel

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.utils.io.use
import androidx.compose.runtime.mutableStateListOf
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.agent.ChatSession
import com.hwj.cook.agent.ChatState
import com.hwj.cook.agent.chatStreaming
import com.hwj.cook.agent.provider.AICookAgentProvider
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.agent.provider.AgentManager
import com.hwj.cook.agent.provider.AgentProvider
import com.hwj.cook.data.local.addMsg
import com.hwj.cook.data.local.fetchMsgs
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.data.repository.SessionRepository
import com.hwj.cook.global.DATA_AGENT_DEF
import com.hwj.cook.global.DATA_AGENT_INDEX
import com.hwj.cook.global.getCacheInt
import com.hwj.cook.global.printLog
import com.hwj.cook.global.saveInt
import com.hwj.cook.global.stopAnswerTip
import com.hwj.cook.global.stopByErrTip
import com.hwj.cook.global.thinkingTip
import com.hwj.cook.global.workInSub
import com.hwj.cook.models.AgentUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import org.koin.core.Koin
import org.koin.core.qualifier.named
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * @author by jason-何伟杰，2025/10/9
 * des:智能体聊天
 */
class ChatVm(
    private val globalRepository: GlobalRepository, private val sessionRepository: SessionRepository
) : ViewModel() {
    private var agentProvider: AgentProvider<String,*>? = null
    private var agentInstance: AIAgent<String, *>? = null
    private val _uiState = MutableStateFlow(
        AgentUiState(
            title = agentProvider?.title,
            //当前会话的消息列表数据
            messages = listOf(ChatMsg.SystemMsg(agentProvider?.description))
        )
    )

    val uiObs: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private val _isAutoScroll: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isAutoScroll: StateFlow<Boolean> = _isAutoScroll.asStateFlow()

    //用以中止当前大模型的过程、包括接口、主动被动
    var curChatJob: kotlinx.coroutines.Job? = null

    private val _currentSessionId: MutableStateFlow<String> =
        MutableStateFlow(Uuid.random().toString())
    val currentSessionState: StateFlow<String> = _currentSessionId.asStateFlow()

    //记录所有单轮会话集合，适用于问答
    private val _sessionObs: MutableStateFlow<MutableList<ChatSession>> =
        MutableStateFlow(mutableListOf())
    val sessionState: StateFlow<MutableList<ChatSession>> = _sessionObs.asStateFlow()

    //停止接收回答
    private val _stopReceivingObs = MutableStateFlow(false)
    val stopReceivingState = _stopReceivingObs.asStateFlow()

    //搞两种模式，文本问答（单轮无上下文）和智能体（多轮带记忆）,适配多种智能体
    private val _agentModelObs = MutableStateFlow(0)
    val agentModelState = _agentModelObs.asStateFlow()

    //所有智能体
    private val _validAgentObs = mutableStateListOf<AgentInfoCell>()
    val validAgentState = MutableStateFlow(_validAgentObs).asStateFlow()
    fun createAgent(koin: Koin, name: String?) {
        if (name == null) {
            agentProvider = AICookAgentProvider()
        } else {
            agentProvider =
                koin.get<AgentProvider<String, String>>(named(name))  //(agentProvider is McpSearchProvider)
        }
    }

    init {
        viewModelScope.launch { //agent=0是问答模式不是智能体
            _agentModelObs.value = getCacheInt(DATA_AGENT_INDEX, 0)
            _validAgentObs.clear()
            _validAgentObs.addAll(AgentManager.validAgentList())
        }
    }

    fun updateInputText(txt: String) {
        _uiState.update { it.copy(inputTxt = txt) }
    }

    fun sendMessage() {
        val userInput = _uiState.value.inputTxt.trim()
        if (userInput.isEmpty()) return

        if (_agentModelObs.value == 0) {
            workInSub {
                runAnswer(userInput)
            }
        } else {
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
            curChatJob = viewModelScope.launch(Dispatchers.Default) { runAgent(userInput) }
        }
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
                var outS:String
                if (result is String){ //List
                    outS=result
                }else{
                    outS="wait>"
                }

                _uiState.update {
                    it.copy(
                        messages = it.messages +
                                ChatMsg.ResultMsg(outS),// + ChatMsg.SystemMsg("The agent has stopped."),
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

    //根据会话id找它的消息列表
    suspend fun findSessionMsg(sessionId: String): MutableList<ChatMsg> {
        val list = mutableListOf<ChatMsg>()
        fetchMsgs(sessionId).collectLatest { list.addAll(it) }
        return list
    }

    suspend fun loadAskSession() {
        _sessionObs.value = sessionRepository.fetchSession()
    }

    suspend fun loadSessionById(sessionId: String) {
        _currentSessionId.value = sessionId

        val flow: Flow<List<ChatMsg>> = fetchMsgs(_currentSessionId.value)
        flow.collectLatest { list ->//从缓存中捞会话消息数据，加载到意图中，意图热加载触发ui更新
            _uiState.update { it.copy(messages = list) }
        }

        _isAutoScroll.value = true
        delay(1000)
        _isAutoScroll.value = false
    }

    //构建新的会话并缓存，标题是输入
    suspend fun addSession(input: String) {
        val session = ChatSession(title = input)
        sessionRepository.addSession(session) //本地缓存
        val sessionList = _sessionObs.value.toMutableList()
        sessionList.add(0, session)
        _sessionObs.value = sessionList
    }

    //新建会话
    fun createSession() {
        _currentSessionId.value = Uuid.random().toString()
    }

    private suspend fun runAnswer(userInput: String) { //问答流式
        _stopReceivingObs.value = false

        //先根据sessionId找消息队列，如果空则要新建对话
        if (findSessionMsg(_currentSessionId.value).isEmpty()) {
            workInSub {
                addSession(userInput)
            }
        }
        val userMsg = ChatMsg.UserMsg(userInput).apply {
            sessionId = _currentSessionId.value
            state = ChatState.Idle
        }
        val newMsg = ChatMsg.ResultMsg(thinkingTip).apply {
            sessionId = _currentSessionId.value
            state = ChatState.Thinking
        }

        val list = mutableListOf<ChatMsg>()
        list.addAll(_uiState.value.messages)
        list.add(0, userMsg)
        list.add(1, newMsg)

        _uiState.update { it.copy(messages = list) }

        //抽出来是为了后面做重新生成
        callAgentApi(userMsg, newMsg)
    }

    //还差apiKey没搞
    private fun callAgentApi(userMsg: ChatMsg.UserMsg, resultMsg: ChatMsg.ResultMsg) {
        curChatJob = viewModelScope.launch {
            var responseFromAgent = ""
            chatStreaming(
                prompt = createPrompt(_uiState.value.messages), onStart = {},
                onCompletion = { cause ->
                    stopReceiveMsg(userMsg, resultMsg.txt, cause)
                }, catch = {
                    if (responseFromAgent == "") {
                        responseFromAgent = stopByErrTip
                        updateLocalResponse(responseFromAgent)
                    }
                }, streaming = { chunk: StreamFrame ->
                    when (chunk) {
                        is StreamFrame.Append -> {
                            responseFromAgent = chunk.text
                        }

                        is StreamFrame.ToolCall -> {
                            printLog("\n Tool call:${chunk.name} args=${chunk.content} ")
                        }

                        is StreamFrame.End -> {
                            printLog("\n[END] reason=${chunk.finishReason}")
                        }
                    }
                    updateLocalResponse(responseFromAgent)
                })
        }
    }

    private fun createPrompt(list: List<ChatMsg>): Prompt {
        return prompt(_currentSessionId.value) {
            list.forEach { msg ->
                when (msg) {
                    is ChatMsg.UserMsg -> user(msg.txt)
                    is ChatMsg.AgentMsg -> assistant(msg.txt)
                    is ChatMsg.SystemMsg -> msg.txt?.let { system(it) }
                    is ChatMsg.ErrorMsg -> msg.txt?.let { assistant(it) }
                    is ChatMsg.ToolCallMsg -> msg.txt?.let { assistant(it) }
                    is ChatMsg.ResultMsg -> assistant(msg.txt)
                }
            }
        }
    }

    private suspend fun stopReceiveMsg(
        userMsg: ChatMsg.UserMsg,
        response: String,
        cause: Throwable?
    ) {
        var tmpAnswer = response
        if (cause is CancellationException) {
            _stopReceivingObs.value = true
            if (response.isEmpty()) {
                tmpAnswer = stopAnswerTip
                updateLocalResponse(tmpAnswer)
            }
        }
        addMsg(userMsg)
        val assistantMsg = ChatMsg.ResultMsg(tmpAnswer).apply {
            sessionId = userMsg.sessionId
        }
        addMsg(assistantMsg)
    }

    //主动中断大模型api逻辑
    fun stopReceivingResults() {
        _stopReceivingObs.value = true
        curChatJob?.cancel()
        curChatJob = null
    }

    //问答的信息肯定在顶部，UI会令消息倒序显示
    fun updateLocalResponse(response: String) {
        val msgList = _uiState.value.messages.toMutableList()
        msgList[1] = (msgList[1] as ChatMsg.ResultMsg).copy(txt = response)
        _uiState.update { it.copy(messages = msgList) }
    }

    fun updateAgentModel() { //index=0是问答模式，其他是各类agent
        viewModelScope.launch {
            _agentModelObs.value = getCacheInt(DATA_AGENT_INDEX)
        }
    }

    suspend fun changeChatModel(model: Int) {
        if (model == 0) {
            saveInt(DATA_AGENT_DEF, _agentModelObs.value)
            _agentModelObs.value = 0
        } else {
            _agentModelObs.value = model
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

    fun deleteSession(sessionId: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId, onFinished = onFinished)

            val sessions = _sessionObs.value.toMutableList()
            val mSession = sessions.find { it.id == sessionId }
            if (mSession != null) {
                sessions.remove(mSession)
                _sessionObs.value = sessions
            }
        }
    }
}