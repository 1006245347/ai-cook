package com.hwj.cook.ui.viewmodel

import ai.koog.embeddings.local.LLMEmbedder
import ai.koog.prompt.dsl.prompt
import androidx.compose.runtime.mutableStateListOf
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.buildEmbedder
import com.hwj.cook.agent.buildIndexJson
import com.hwj.cook.agent.createRootDir
import com.hwj.cook.agent.provider.MemoryAgentProvider
import com.hwj.cook.buildFileStorage
import com.hwj.cook.deleteRAGFile
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MEMORY_INPUT
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.getMills
import com.hwj.cook.global.printD
import com.hwj.cook.global.printList
import com.hwj.cook.global.saveString
import com.hwj.cook.global.truncate
import com.hwj.cook.models.FileInfoCell
import com.hwj.cook.models.IndexFile
import com.hwj.cook.models.LocalIndex
import com.hwj.cook.models.MemoryUiState
import com.hwj.cook.storeFile
import io.github.alexzhirkevich.compottie.InternalCompottieApi
import io.github.alexzhirkevich.compottie.createFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

/**
 * @author by jason-何伟杰，2025/10/13
 * des:agent存事实、某类主题、应用范围的记忆
 */
@OptIn(InternalCompottieApi::class)
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
        loadRagReqFile()
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
        file?.let { f ->
            if (_fileInfoListObs.none { f.path == it.path }) {//是否已经添加过文件
                val info = FileInfoCell(
                    path = f.path,
                    documentId = null,
                    name = f.name,
                    millDate = getMills(),
                    fileSize = f.size(),
                    isEmbed = false
                )
                _fileInfoListObs.add(info)
                ragStorage(f.path, info.id)
            } else {
                ToastUtils.show("已经添加过${f.name}".truncate(20))
            }
        }
    }

    //向量化一个文件
    suspend fun ragStorage(filePath: String, id: String) {
        if (llmEmbedder == null) {
            llmEmbedder = buildEmbedder(getCacheString(DATA_APP_TOKEN)!!)
            buildFileStorage(createRootDir("embed/index"), llmEmbedder!!)
        }

        ///Users/jasonmac/Library/Application Support/AI_COOK/embed/index/vectors/c06275ff-2e7c-4fc2-a953-57d623323622
        storeFile(filePath) { documentId -> //直接以id作为文件名存了
            viewModelScope.launch(Dispatchers.IO) {

                val indexFilePath = buildIndexJson()
                if (!PlatformFile(indexFilePath).exists()) {
                    //创建文件 向量化过的索引表
                    FileSystem.SYSTEM.createFile(indexFilePath.toPath())
                }

                val json = PlatformFile(indexFilePath).readString() //读取文件内容
                val srcFile = PlatformFile(filePath)
                val itemFile = IndexFile(
                    id = id,
                    documentId = documentId,
                    absolutePath = filePath,
                    fileName = srcFile.name,
                    filePath = srcFile.absolutePath(),
                    fileType = srcFile.extension,
                    fileSize = srcFile.size(),
                    millDate = getMills(),
                    isEmbed = false,
                    fileHash = null
                )
                if (!json.isEmpty()) {
                    val indexRoot: LocalIndex = JsonApi.decodeFromString<LocalIndex>(json)
                    val indexFiles = indexRoot.indexedFiles

                    if (indexFiles == null || indexFiles.isEmpty()) {
//                        val mList = mutableListOf<IndexFile>()
                        //向量化后的文件名都为新路径+id,看看删除会成功不
                        indexRoot.indexedFiles = mutableListOf(itemFile)
                    } else { //续旧的
                        indexFiles.add(itemFile)
                    }
                    val cache = JsonApi.encodeToString(indexRoot)
                    PlatformFile(indexFilePath).writeString(cache) //覆盖文本
                } else {
                    val indexRoot = LocalIndex()
                    indexRoot.indexedFiles = mutableListOf(itemFile)
                    val cache = JsonApi.encodeToString(indexRoot)
                    PlatformFile(indexFilePath).writeString(cache)
                }
            }
        }
    }

    suspend fun loadRagReqFile() {
        try {
            val indexFilePath = buildIndexJson()
            if (PlatformFile(indexFilePath).exists()) {
                val json = PlatformFile(indexFilePath).readString()
                val indexRoot = JsonApi.decodeFromString<LocalIndex>(json)
                if (!json.isEmpty()) {
                    val indexFiles = indexRoot.indexedFiles
                    if (indexFiles != null && indexFiles.isNotEmpty()) {
                        val list = mutableListOf<FileInfoCell>()
                        indexFiles.forEach { item ->
                            list.add(
                                FileInfoCell(
                                    id = item.id,
                                    path = item.filePath!!,
                                    documentId = item.documentId,
                                    name = item.fileName!!,
                                    millDate = item.millDate!!,
                                    fileSize = item.fileSize!!,
                                    isEmbed = item.isEmbed!!
                                )
                            )
                        }
                        _fileInfoListObs.clear()
                        _fileInfoListObs.addAll(list)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectRagFile(isSelected: Boolean, file: FileInfoCell) {
        if (isSelected) {
            if (_selectedFileObs.none { file.path == it.path && file.id == it.id }) {
                _selectedFileObs.add(file)
            }
        } else {
            val item = _selectedFileObs.firstOrNull { it.path == file.path }
            _selectedFileObs.remove(item)
        }
    }

    fun deleteRagFile() { //删除选中的文件
        viewModelScope.launch(Dispatchers.IO) {
            _selectedFileObs.forEach { item ->
//                PlatformFile(it.path).delete() //不是删源文件呀
                if (llmEmbedder == null) {
                    llmEmbedder = buildEmbedder(getCacheString(DATA_APP_TOKEN)!!)
                    buildFileStorage(createRootDir("embed/index"), llmEmbedder!!)
                }
                printD("delete>${item.documentId}")
                item.documentId?.let {
                    deleteRAGFile(it)
                }
            }
            //index.json也要删
            updateIndexJson(_selectedFileObs)
            //删除某个元素的属性值同时在两个集合相同的元素
            val list2 = _selectedFileObs.map { it.id }.toSet()

            _fileInfoListObs.removeAll { it.path in list2 }
            _selectedFileObs.clear()

        }
    }

    suspend fun updateIndexJson(deleteList: List<FileInfoCell>) {
        val indexFilePath = buildIndexJson()
        if (!PlatformFile(indexFilePath).exists()) {
            FileSystem.SYSTEM.createFile(indexFilePath.toPath())
        }
        val json = PlatformFile(indexFilePath).readString() //读取文件内容
        if (!json.isEmpty()) {
            val indexRoot: LocalIndex = JsonApi.decodeFromString<LocalIndex>(json)
            //组合条件，元素同时满足条件切都在两个list存在
            val list = deleteList.map { it.id to it.path }.toSet()
            printList(list.toList(),"index?")
            indexRoot.indexedFiles?.removeAll { it.id to it.absolutePath in list }
            val cache = JsonApi.encodeToString(indexRoot)
            PlatformFile(indexFilePath).writeString(cache)
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