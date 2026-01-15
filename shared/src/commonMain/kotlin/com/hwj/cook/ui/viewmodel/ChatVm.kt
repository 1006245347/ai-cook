@file:OptIn(ExperimentalUuidApi::class)

package com.hwj.cook.ui.viewmodel

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.utils.io.use
import androidx.compose.runtime.mutableStateListOf
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.agent.ChatSession
import com.hwj.cook.agent.ChatState
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.agent.chatStreaming
import com.hwj.cook.agent.provider.AICookAgentProvider
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.agent.provider.AgentManager
import com.hwj.cook.agent.provider.AgentProvider
import com.hwj.cook.data.local.addMsg
import com.hwj.cook.data.local.fetchMsgList
import com.hwj.cook.data.local.fetchMsgListFlow
import com.hwj.cook.data.local.isNewSession
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.data.repository.SessionRepository
import com.hwj.cook.except.ClipboardHelper
import com.hwj.cook.global.DATA_AGENT_DEF
import com.hwj.cook.global.DATA_AGENT_INDEX
import com.hwj.cook.global.defSystemTip
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printE
import com.hwj.cook.global.printList
import com.hwj.cook.global.printLog
import com.hwj.cook.global.removeCacheKey
import com.hwj.cook.global.saveString
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
    private val globalRepository: GlobalRepository,
    private val sessionRepository: SessionRepository,
    private val clipboardHelper: ClipboardHelper
) : ViewModel() {
    private var agentProvider: AgentProvider<String, *>? = null
    private var agentInstance: AIAgent<String, *>? = null
    private val _uiState = MutableStateFlow(
        AgentUiState(
            title = agentProvider?.title,
            //当前会话的消息列表数据,是反序的
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

    //停止接收回答 ,和_uiObs.isLoading有重复作用
    private val _stopReceivingObs = MutableStateFlow(false)
    val stopReceivingState = _stopReceivingObs.asStateFlow()

    //搞两种模式，文本问答（单轮无上下文）和智能体（多轮带记忆）,适配多种智能体
    private val _agentModelObs = MutableStateFlow<String?>(null)
    val agentModelState = _agentModelObs.asStateFlow()

    //所有智能体
    private val _validAgentObs = mutableStateListOf<AgentInfoCell>()
    val validAgentState = MutableStateFlow(_validAgentObs).asStateFlow()
    suspend fun createAgent(koin: Koin, name: String?) {
        saveString(DATA_AGENT_INDEX, name ?: "cook")
        _agentModelObs.value = name ?: "cook"
        agentProvider = if (name == null) {
            AICookAgentProvider()
        } else {
            koin.get<AgentProvider<String, String>>(named(name))  //(agentProvider is McpSearchProvider)
        }
    }

    init {  //这个创建时间顺序影响逻辑吧？
        viewModelScope.launch { //agent=null是问答模式不是智能体
            _agentModelObs.value = getCacheString(DATA_AGENT_INDEX)

            _validAgentObs.clear()
            _validAgentObs.addAll(AgentManager.validAgentList())
            createSession()

            _uiState.update {
                it.copy(
                    title = if (isLLMAsk()) "Ask" else agentProvider?.title,
                    messages = listOf(ChatMsg.SystemMsg(if (isLLMAsk()) defSystemTip else agentProvider?.description))
                )
            }
        }
    }

    fun updateInputText(txt: String) {
        _uiState.update { it.copy(inputTxt = txt) }
    }

    fun sendMessage() {
        val userInput = _uiState.value.inputTxt.trim()
        if (userInput.isEmpty()) return

        if (_agentModelObs.value.isNullOrEmpty()) {
            viewModelScope.launch {
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
            var answerFromGPT = ""
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
            }, onLLMStreamFrameEvent = { frame ->//流式
                answerFromGPT += frame
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMsg.ResultMsg(answerFromGPT),
                        isInputEnabled = false, isLoading = false, isChatEnded = true
                    )
                }
            })

            agentInstance?.use { _agent ->
                val result = _agent.run(userInput)
                var outS = ""
                if (result is String) { //List
                    outS = result
                } else if (result is List<*>) {
                    if (result.all { it is Message.Response }) {
                        val list = result.filterIsInstance<Message.Response>()
                        printList(list, "chat done?")
                    }
                    outS = "wait>"
                }

                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMsg.ResultMsg(outS),// + ChatMsg.SystemMsg("The agent has stopped."),
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
                    messages = it.messages + ChatMsg.ErrorMsg("error2:${e.message}"),
                    isInputEnabled = true, isLoading = false
                )
            }
            printD(e.message)
        }
    }

    //根据会话id找它的消息列表
    suspend fun findSessionMsg(sessionId: String): List<ChatMsg> {
        return fetchMsgList(sessionId)
    }

    suspend fun loadAskSession() {
        _sessionObs.value = sessionRepository.fetchSession() //这里反序处理了
    }

    suspend fun loadSessionById(sessionId: String) {
        _currentSessionId.value = sessionId

        val flow: Flow<List<ChatMsg>> = fetchMsgListFlow(_currentSessionId.value)
        flow.collectLatest { list ->//从缓存中捞会话消息数据，加载到意图中，意图热加载触发ui更新
            _uiState.update {
                it.copy(
                    messages = list, isLoading = false, isInputEnabled = true,
                    isChatEnded = false
                )
            } //界面上用了反序
            printList(list, "loadSession>$sessionId")
        }

        _isAutoScroll.value = true
        delay(1000)
        _isAutoScroll.value = false
    }

    //构建新的会话并缓存，标题是输入
    suspend fun addSession(input: String) {
        try {
            val session = ChatSession(
                title = if (input.length > 20) input.take(20) else input,
                id = _currentSessionId.value
            )
            sessionRepository.addSession(session) //本地缓存
            val sessionList = _sessionObs.value.toMutableList()
            sessionList.add(0, session)
            _sessionObs.value = sessionList
        } catch (e: Exception) {
            printD(e.message, "addSession")
        }
    }

    //新建会话
    fun createSession() {
        _currentSessionId.value = Uuid.random().toString()
        _uiState.value = AgentUiState() //重置界面数据
    }

    suspend fun runAnswer(userInput: String) { //问答流式
        _stopReceivingObs.value = false
        try {
            //先根据sessionId找消息队列，如果空则要新建对话
            if (findSessionMsg(_currentSessionId.value).isEmpty() &&
                isNewSession(_currentSessionId.value, _sessionObs.value)
            ) {
                workInSub {
                    addSession(userInput)
                }
            }
        } catch (e: Exception) {
            printD(e.message)
        }

        val userMsg = ChatMsg.UserMsg(userInput).apply {
            sessionId = _currentSessionId.value
            state = ChatState.Idle
        }
        val resultMsg = ChatMsg.ResultMsg(thinkingTip).apply {
            sessionId = _currentSessionId.value //提示思考中，但是不要给接口用
            state = ChatState.Thinking
        }

        //搞倒序缓存才行，新的在顶，旧的在低，显示时再全反转
        val list = mutableListOf<ChatMsg>()
        list.addAll(_uiState.value.messages)
        list.add(0, resultMsg)
        list.add(1, userMsg)

        _uiState.update {
            it.copy(
                messages = list,
//                inputTxt = "",回复了再清更好？
                isInputEnabled = false,
                isLoading = true
            )
        }
        //抽出来是为了后面做重新生成
        callLLMAnswer(userMsg)
    }

    private fun callLLMAnswer(userMsg: ChatMsg.UserMsg) {
        curChatJob = viewModelScope.launch {
            var responseFromAgent = ""
            val tmpList = if (_uiState.value.messages.first() is ChatMsg.ResultMsg) {
                _uiState.value.messages.drop(1)
            } else {
                _uiState.value.messages
            }
            chatStreaming(
                prompt = createPrompt(
                    tmpList.reversed(), //请求数据时要正序
                    params = LLMParams(
                        temperature = 0.8,
                        //没用，这样的设定不认
//                        additionalProperties = mapOf("enable_thinking" to JsonPrimitive(false))
                    )
                ), //qwen关闭深度思考
                llModel = buildQwen3LLM(), onStart = {},
                onCompletion = { cause -> //手动停止进这; 自动结束也进但是cause=null; 连不上网进这；
                    printE(cause, des = "onCompletion1")//会不会StreamFrame.End重复添加
                    workInSub { //不切换线程貌似不执行,应该是同一个job
                        stopReceiveMsg(userMsg, responseFromAgent, cause)
                    }
                }, catch = { e ->// 断网
                    _stopReceivingObs.value = true
                    if (responseFromAgent == "") {
                        responseFromAgent = stopByErrTip
                        updateLocalResponse(responseFromAgent)
                    }
                }, streaming = { chunk: StreamFrame ->
                    when (chunk) {
                        is StreamFrame.Append -> {
                            responseFromAgent += chunk.text
                        }

                        is StreamFrame.ToolCall -> {
                            printLog("Tool call:${chunk.name} args=${chunk.content} ")
                        }

                        is StreamFrame.End -> { //正常结束也会进这
                            printLog("[END] reason=${chunk.finishReason}")
                        }
                    }
                    updateLocalResponse(responseFromAgent.trim())
                })
        }
    }

    private fun createPrompt(list: List<ChatMsg>, params: LLMParams = LLMParams()): Prompt {
        return prompt(id = _currentSessionId.value, params = params) {
            list.forEach { msg ->
                when (msg) {
                    is ChatMsg.UserMsg -> user(msg.txt)
                    is ChatMsg.AgentMsg -> assistant(msg.txt)
                    is ChatMsg.SystemMsg -> msg.txt?.let {
                        system(it)
                    } //界面一般不显示,这里是数据构造
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
        val assistantMsg = ChatMsg.ResultMsg(tmpAnswer).apply {
            sessionId = userMsg.sessionId
        }

        // us2 as2 ,us1 as1  user as ,这里的顺序有问题 存的
        addMsg(userMsg)
        addMsg(assistantMsg)
        _uiState.update { it.copy(isLoading = false, isInputEnabled = true, isChatEnded = false) }
    }

    //主动中断大模型api逻辑
    fun stopReceivingResults() {
        _stopReceivingObs.value = true
        _uiState.update {
            it.copy(
                isLoading = false,
                isInputEnabled = true,
                isChatEnded = false,
                userResponseRequested = false
            )
        }
        //这样停止，把保存动作也停了？ 改用executor
        curChatJob?.cancel() //直接停止了client
        curChatJob = null
    }

    //问答的信息肯定在顶部，UI会令消息倒序显示
    fun updateLocalResponse(response: String) {
        val msgList = _uiState.value.messages.toMutableList() //这里的数据是倒序的结果
        msgList[0] = (msgList[0] as ChatMsg.ResultMsg).copy(txt = response)
        _uiState.update {
            it.copy(
                messages = msgList, inputTxt = "",
                isInputEnabled = true,
                userResponseRequested = false,
            )
        }
    }

    suspend fun changeChatModel(model: String?) {
        if (model == null) { //切到ask
            if (_agentModelObs.value != null) {
                saveString(DATA_AGENT_DEF, _agentModelObs.value!!)
            }
            removeCacheKey(DATA_AGENT_INDEX)
            _agentModelObs.value = null
        } else {
            //从ask切到agent,当前model就是下次的默认
            _agentModelObs.value = model
            saveString(DATA_AGENT_INDEX, model)
            saveString(DATA_AGENT_DEF, model)
        }
    }

    fun isLLMAsk(): Boolean {
        return _agentModelObs.value == null
    }

    //返回历史agent
    suspend fun getCacheAgent(): String {
        return if (isLLMAsk()) { //是ask模式，那么找历史agent
            val def = getCacheString(DATA_AGENT_DEF)
            getCacheString(DATA_AGENT_DEF, def ?: "cook")!!
        } else { //直接给当前agent
            getCacheString(DATA_AGENT_INDEX, "cook")!!
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
            if (sessionId == _currentSessionId.value) {
                stopReceivingResults()
                createSession()
            }
        }
    }

    fun copyToClipboard(text: String) {
        try {
            clipboardHelper.copyToClipboard(text)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateMsgAgain() {}
    fun test() {
        viewModelScope.launch {
            printList(_sessionObs.value, "all-session")
//            clearCache()
        }
    }
}