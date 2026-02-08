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
import com.hwj.cook.agent.tools.testMcp11
import com.hwj.cook.agent.tools.testMcp2
import com.hwj.cook.data.local.addAllMsg
import com.hwj.cook.data.local.addMsg
import com.hwj.cook.data.local.fetchMsgList
import com.hwj.cook.data.local.fetchMsgListFlow
import com.hwj.cook.data.local.isNewSession
import com.hwj.cook.data.repository.CookBookRepository
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.data.repository.SessionRepository
import com.hwj.cook.except.ClipboardHelper
import com.hwj.cook.global.DATA_AGENT_DEF
import com.hwj.cook.global.DATA_AGENT_INDEX
import com.hwj.cook.global.DATA_BOOK_ROOT
import com.hwj.cook.global.clearCache
import com.hwj.cook.global.defAgentLabel
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
import com.hwj.cook.listResourceFiles
import com.hwj.cook.models.AgentUiState
import com.hwj.cook.platformAgentTools
import com.hwj.cook.runLiteWork
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
import kotlin.text.get
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
    private val _uiState = MutableStateFlow(buildDefState())

    //在智能体模式下，涉及工具，如果继续把请求数据集放到uiState,很难保持正确，单独搞个数据源吧
    private val promptMessages = mutableListOf<ChatMsg>()
    private val reqPromptMsg = mutableListOf<ChatMsg>()
    private var curPrompt: Prompt? = null

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

    init {  //这个创建时间顺序影响逻辑吧？
        viewModelScope.launch { //agent=null是问答模式不是智能体
            _agentModelObs.value = getCacheString(DATA_AGENT_INDEX)

            _validAgentObs.clear()
            _validAgentObs.addAll(AgentManager.validAgentList())
            createSession()
        }
    }

    suspend fun createAgent(koin: Koin, name: String?) {
        saveString(DATA_AGENT_INDEX, name ?: defAgentLabel)
        _agentModelObs.value = name ?: defAgentLabel
        agentProvider = if (name == null) {
            koin.get<AgentProvider<String, String>>(named(defAgentLabel))
        } else if (name == "suggest" || name == "cook") { //流式
            koin.get<AgentProvider<String, List<Message.Response>>>(named(name))  //(agentProvider is McpSearchProvider)
        } else {
            koin.get<AgentProvider<String, String>>(named(name))  //(agentProvider is McpSearchProvider)
        }
        _uiState.update {
            it.copy(
                title = if (isLLMAsk()) "Ask" else agentProvider?.title,
                messages = listOf(ChatMsg.SystemMsg(if (isLLMAsk()) defSystemTip else agentProvider?.description))
            )
        }
        promptMessages.clear()
        //界面、数据是分开的效果
        promptMessages += ChatMsg.SystemMsg(agentProvider?.description)
        reqPromptMsg.clear()
        reqPromptMsg += ChatMsg.SystemMsg(agentProvider?.description)
    }

    fun updateInputText(txt: String) {
        _uiState.update { it.copy(inputTxt = txt) }
    }

    fun sendMessage() {
        val userInput = _uiState.value.inputTxt.trim()
        if (userInput.isEmpty()) return
        _stopReceivingObs.value = false

        //诡异，存在agent构建失败null，导致都没执行

        viewModelScope.launch(Dispatchers.Default) {
            try {
                //先根据sessionId找消息队列，如果空则要新建对话
                if (findSessionMsg(_currentSessionId.value).isEmpty() && isNewSession(
                        _currentSessionId.value,
                        _sessionObs.value
                    )
                ) {
                    addSession(userInput) //
                }
            } catch (e: Exception) {
                printD(e.message)
            }
        }
        if (_agentModelObs.value.isNullOrEmpty()) { //是否是问答模式
            curChatJob = viewModelScope.launch { runAnswer(userInput) }
        } else {
            val userMsg = ChatMsg.UserMsg(userInput).apply { sessionId = _currentSessionId.value }
            if (_uiState.value.userResponseRequested) { //回复智能体的问题，用户再输入

                _uiState.update {
                    val ll = it.messages.toMutableList().apply {
                        add(0, ChatMsg.ResultMsg(thinkingTip).apply {
                            sessionId = _currentSessionId.value
                        })
                        add(1, userMsg)
                    }
                    it.copy(
                        messages = ll,
                        isLoading = true,
                        userResponseRequested = false,
                        currentUserResponse = userInput
                    )
                }
            } else {  //init msgList , start agent, systemMsg+userMsg 翻转
                //不应该这样存数据了，koog可以管理
                _uiState.update {
                    val ll = it.messages.toMutableList().apply {
                        add(0, ChatMsg.ResultMsg(thinkingTip).apply {
                            sessionId = _currentSessionId.value
                            state = ChatState.Thinking
                        })
                        add(1, userMsg.apply {
                            state = ChatState.Idle
                        })
                    }
                    it.copy(
                        messages = ll, isInputEnabled = false, isLoading = true
                    )
                }
//                promptMessages += userMsg //不应该加，agent.run会加一次 ,这再搞个备用的list?
                reqPromptMsg += userMsg
            }
            curChatJob = viewModelScope.launch(Dispatchers.Default) { runAgent(userMsg) }
        }
    }

    private suspend fun runAgent(userMsg: ChatMsg.UserMsg) {
        var responseFromAgent = ""
        try {
            agentInstance = agentProvider?.provideAgent(
                prompt = createPrompt(promptMessages),
                onToolCallEvent = { msg ->
                    viewModelScope.launch {
                        _uiState.update {
                            it.copy(
                                messages = it.messages.toMutableList()
                                    .apply { add(1, ChatMsg.ToolCallMsg(msg)) })
                        }

                    }
//                    promptMessages += ChatMsg.ToolCallMsg(msg)
                    reqPromptMsg += ChatMsg.ToolCallMsg(msg)
                }, onToolResultEvent = { msg ->
//                    promptMessages += ChatMsg.ToolResultMsg(msg)
                    reqPromptMsg += ChatMsg.ToolResultMsg(msg)
                },
                onErrorEvent = { errorMsg ->
                    if (!errorMsg.contains("StandaloneCoroutine was cancelled")) {
                        viewModelScope.launch {
                            val tmpList = _uiState.value.messages.toMutableList()
                            tmpList.removeFirst()
                            _uiState.update {
                                it.copy(
                                    messages = tmpList.apply {
                                        add(0, ChatMsg.ErrorMsg(errorMsg))
                                    },
                                    isInputEnabled = true,
                                    isLoading = false
                                )
                            }
                            stopReceiveMsg(userMsg, responseFromAgent, null)
                        }
                    }
                }, onAssistantMessage = { agentQuestion ->
//                    printD("onAssistantMessage>$agentQuestion")
                    val msgList = _uiState.value.messages.toMutableList()
                    msgList.removeFirst() //现在第一个应该是loading
                    msgList.add(0, ChatMsg.AgentMsg(agentQuestion))
                    _uiState.update {
                        it.copy(
                            messages = msgList, inputTxt = "",
                            isInputEnabled = true,
                            isLoading = false,
                            userResponseRequested = true
                        )
                    }
                    //大模型在会话当中主动提问  //这里有点问题？到底那类型好？Result?Agent
//                    promptMessages += ChatMsg.AgentMsg(agentQuestion)
                    reqPromptMsg += ChatMsg.AgentMsg(agentQuestion)
                    //在这block中agent没结束,但问题是会导致重复加入吗
                    reqPromptMsg.drop(promptMessages.size) //丢前几个
                        .let { extra -> if (extra.isNotEmpty()) promptMessages.addAll(extra) }


                    // Wait for user response
                    val userResponse =
                        _uiState.first { it.currentUserResponse != null }.currentUserResponse
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
                    responseFromAgent += frame
                    updateLocalResponse(responseFromAgent)
                })

//            printD("agent=$agentInstance p=$agentProvider")
            //这里是非流式返回
            //有个逻辑问题，这里run会自动prompt添加userMsg,如果我再promptMessage再手动加会重复
            responseFromAgent = agentInstance?.run(userMsg.txt).toString()
//                agentInstance.agentConfig.prompt.messages.also {
//                    printList(it, "promptList")
//                }
            printD("response done.-->stopReceiveMsg")
            stopReceiveMsg(userMsg, responseFromAgent, null)
            //官方每次run后都是重新聊天，其实我们也是，但是会带入历史会话到新的作为上下文
        } catch (e: Exception) {
//            _uiState.update {
//                it.copy(
//                    messages = it.messages.toMutableList().apply {
//                        add(0, ChatMsg.ErrorMsg("error2:${e.message}"))
//                    },
//                    isInputEnabled = true,
//                    isLoading = false
//                )
//            }
            stopReceiveMsg(userMsg, responseFromAgent, e)
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
                    messages = list, isLoading = false, isInputEnabled = true, isChatEnded = false
                )
            } //界面上用了反序
            if (!isLLMAsk()) {
                promptMessages.clear()
                promptMessages += list.reversed()
                reqPromptMsg.clear()
                reqPromptMsg += list.reversed()
            }
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
            e.printStackTrace()
            printD(e.message, "addSession")
        }
    }

    //新建会话 不同模式
    fun createSession() {
        if (isLLMAsk()) agentProvider = null
        promptMessages.clear()
        promptMessages += ChatMsg.SystemMsg(agentProvider?.description)
        reqPromptMsg.clear()
        reqPromptMsg += ChatMsg.SystemMsg(agentProvider?.description)
        _currentSessionId.value = Uuid.random().toString()
        _uiState.value = buildDefState()//重置界面数据
    }

    fun buildDefState() = AgentUiState(
        title = agentProvider?.title,
        //当前会话的消息列表数据,是反序的
        messages = if (agentProvider?.description == null) listOf()
        else listOf(ChatMsg.SystemMsg(agentProvider?.description))
    )

    suspend fun runAnswer(userInput: String) { //问答流式
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
                messages = list,//                inputTxt = "",回复了再清更好？
                isInputEnabled = false, isLoading = true
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
                llModel = buildQwen3LLM(),
                onStart = {},
                onCompletion = { cause -> //手动停止进这; 自动结束也进但是cause=null; 连不上网进这；
                    printE(cause, des = "onCompletion1")//会不会StreamFrame.End重复添加
                    workInSub { //不切换线程貌似不执行,应该是同一个job
                        stopReceiveMsg(userMsg, responseFromAgent, cause)
                    }
                },
                catch = { e ->// 断网
                    _stopReceivingObs.value = true
                    if (responseFromAgent == "") {
                        responseFromAgent = stopByErrTip
                        updateLocalResponse(responseFromAgent)
                    }
                },
                streaming = { chunk: StreamFrame ->
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
                    is ChatMsg.ToolCallMsg -> {
                        tool { call(msg.call) }
                    }

                    is ChatMsg.ToolResultMsg -> {
                        tool { result(msg.result) }
                    }

                    is ChatMsg.ResultMsg -> assistant(msg.txt)
                }
            }
        }.also { curPrompt = it }  //prompt可以累加的，不知道有用不
    }

    private suspend fun stopReceiveMsg(
        userMsg: ChatMsg.UserMsg, response: String, cause: Throwable?
    ) {
        var tmpAnswer = response
        cause?.let {
            _stopReceivingObs.value = true
            if (cause is CancellationException) {
                if (response.isEmpty()) {
                    tmpAnswer = stopAnswerTip
                    updateLocalResponse(tmpAnswer)
                }
            } else if (cause.message!!.contains("Failed to parse HTTP response")) {
                if (response.isEmpty()) {
                    tmpAnswer = stopAnswerTip
                    updateLocalResponse(tmpAnswer)
                }
            }
        }
        if (null == cause) {
            updateLocalResponse(tmpAnswer)
        }
        val assistantMsg = ChatMsg.ResultMsg(tmpAnswer).apply {
            sessionId = userMsg.sessionId
        }

        // us2 as2 ,us1 as1  user as ,这里的顺序有问题 存的
        if (isLLMAsk()) {
            addMsg(userMsg)
            addMsg(assistantMsg)
        } else {

//            promptMessages += assistantMsg //
            reqPromptMsg += assistantMsg //到这步，promptMessage里都没对话的msg，userMsg不能加到prompt,智能体有个自动加会重复
            //这里根据reqPromptMsg恢复下promptMessages ！！！！！！
            reqPromptMsg.drop(promptMessages.size)//丢前n个
                .let { extra -> if (extra.isNotEmpty()) promptMessages.addAll(extra) }

            //不能每次保存都drop出新的list
            addAllMsg(promptMessages.reversed()) //删systemMsg后反序

//            printList(promptMessages)
        }
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
        curChatJob?.cancel()
        curChatJob = null
    }

    //问答的信息肯定在顶部，UI会令消息倒序显示
    fun updateLocalResponse(response: String) {
        val msgList = _uiState.value.messages.toMutableList() //这里的数据是倒序的结果
        //这里很容易报错 errorMsg cast
        if (msgList[0] is ChatMsg.ResultMsg) {
            (msgList[0] as ChatMsg.ResultMsg).copy(txt = response)
        } else if (msgList[0] is ChatMsg.ErrorMsg) {
            (msgList[0] as ChatMsg.ErrorMsg).copy(txt = response)
        }
        _uiState.update {
            it.copy(
                messages = msgList, inputTxt = "",
                isInputEnabled = true, isLoading = false,
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
            getCacheString(DATA_AGENT_DEF, def ?: defAgentLabel)!!
        } else { //直接给当前agent
            getCacheString(DATA_AGENT_INDEX, defAgentLabel)!!
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
//            printList(_sessionObs.value, "all-session")
//            clearCache()
            runLiteWork {
//                viewModelScope.launch {
//                    val rootPath = getCacheString(DATA_BOOK_ROOT)!!
//                    listResourceFiles(rootPath).also { bookNode ->
//                        CookBookRepository.loadCookBookVector(bookNode)
//                    }
//                }
            }
//            testMcp2()
//            printList(promptMessages, "ppp")
//            delay(100)
//            printList(reqPromptMsg, "req")
//            printList(agentInstance?.agentConfig?.prompt?.messages,"config")

        }
    }
}