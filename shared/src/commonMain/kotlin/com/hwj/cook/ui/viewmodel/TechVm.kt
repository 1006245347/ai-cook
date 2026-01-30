package com.hwj.cook.ui.viewmodel

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.prompt
import androidx.compose.runtime.mutableStateListOf
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildEmbedder
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.MemoryAgentProvider
import com.hwj.cook.buildFileStorage
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MEMORY_INPUT
import com.hwj.cook.global.DATA_RAG_FILE
import com.hwj.cook.global.getCacheList
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.getMills
import com.hwj.cook.global.printD
import com.hwj.cook.global.saveString
import com.hwj.cook.models.FileInfoCell
import com.hwj.cook.models.MemoryUiState
import com.hwj.cook.storeFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.size
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

    //处理选择的需要被向量化的文件
    private val _fileInfoListObs = mutableStateListOf<FileInfoCell>()
    val fileInfoListState = MutableStateFlow(_fileInfoListObs).asStateFlow()

    //已选择的向量化文件，知识库文件
    private val _selectedFileObs = mutableStateListOf<FileInfoCell>()
    val selectedFileState = MutableStateFlow(_selectedFileObs).asStateFlow()

    private var agentProvider: MemoryAgentProvider? = null

    private var llmEmbedder: LLMEmbedder? = null

    suspend fun initialize() {
        //koog的rag会复制一份新的
        ragReqFile()
    }

    fun updateInputText(txt: String) { //更新了输入了呀
        _uiState.update { it.copy(inputTxt = txt) }
    }

    suspend fun loadInputCache() {
        //把输入的内容存下来，大模型的记忆不对外显示
        getCacheString(DATA_MEMORY_INPUT, "")?.let {
            updateInputText(it)
        }
    }

    fun sendFact2Memory() {
        val userInput = _uiState.value.inputTxt.trim()
        if (userInput.isEmpty()) return

        _uiState.update { it.copy(inputTxt = "", isLoading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            runAgent(userInput)
        }
    }

    private suspend fun runAgent(userInput: String) {
        try {
            val agent = agentProvider?.provideAgent(prompt = prompt(id = "tech") {
                system("A conversational agent that supports long-term memory, with clear and concise responses.")
            }, {}, {}, onErrorEvent = { errorMsg ->
                _uiState.update { it.copy(isLoading = false) }
            }, onLLMStreamFrameEvent = {}, onAssistantMessage = { "" })
            val result = agent?.run(userInput)
            _uiState.update {
                it.copy(
                    isInputEnabled = true,
                    isLoading = false,
                    memoryOfUser = result
                )
            }
            saveString(DATA_MEMORY_INPUT, userInput)
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update {
                it.copy(isLoading = false, isInputEnabled = true)
            }
        }
    }

    suspend fun chooseFile() {
        val mode = FileKitMode.Single
        val list = listOf("txt", "md")
        val file =
            FileKit.openFilePicker(type = FileKitType.File(extensions = list), mode = mode)
        printD("file?$file")
        file?.let { f ->
            printD("f>${f.name}")
            if (_fileInfoListObs.none { f.path == file.path }) {
                val info = FileInfoCell(f.path, f.name, getMills(), f.size(), false)
                _fileInfoListObs.add(info)
                saveString(DATA_RAG_FILE, JsonApi.encodeToString(_fileInfoListObs.toList()))//bug
                ragStorage(f.path)
            }
        }
    }

    suspend fun ragStorage(filePath: String) {
        printD("start>ragStorage")
        if (llmEmbedder == null) {
            llmEmbedder = buildEmbedder(getCacheString(DATA_APP_TOKEN)!!)
            buildFileStorage(createRootDir("embed/index"))
        }

        storeFile(filePath) { id ->
            printD("id=$id $filePath")
        }
    }

    suspend fun ragReqFile() {
        try {
            _fileInfoListObs.clear()
            val list = getCacheList<FileInfoCell>(DATA_RAG_FILE)
            list?.let { _fileInfoListObs.addAll(list) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectRagFile(path: String) {
        val item = _fileInfoListObs.firstOrNull { it.path == path }
        item?.let {
            _selectedFileObs.add(it)
        }
    }

    fun deleteRagFile() {
        viewModelScope.launch {
            _selectedFileObs.forEach {
                PlatformFile(it.path).delete()
            }
            //删除某个元素的属性值同时在两个集合相同的元素
            val list2 = _selectedFileObs.map { it.path }.toSet()
            _fileInfoListObs.removeAll { it.path in list2 }
            _selectedFileObs.clear()
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