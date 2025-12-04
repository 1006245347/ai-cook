package com.hwj.cook.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.global.DATA_MODEL_LIST
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printLog
import com.hwj.cook.global.removeCacheKey
import com.hwj.cook.global.saveString
import com.hwj.cook.models.ModelInfoCell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

class SettingVm : ViewModel() {

    private val _modelsObs = mutableStateListOf<ModelInfoCell>()
    val modelsState = MutableStateFlow(_modelsObs).asStateFlow()

    fun initialize() {
        viewModelScope.launch {
            val cache = getCacheString(DATA_MODEL_LIST)
            cache?.let {
                val list = JsonApi.decodeFromString<MutableList<ModelInfoCell>>(it)
                if (!list.isEmpty()) {
                    _modelsObs.clear()
                    _modelsObs.addAll(list)
                }
            }
            autoAddModel()
        }
    }

    suspend fun autoAddModel() {
        //得搞个默认的
        if (_modelsObs.isEmpty()) {
            addModel(
                "gpt-4o",
                "apikey",//得换
                "gpt-4o",
                "https://baitong-it.gree.com",
                "aicodeOpen/baitong/chat/completions",
                "https://baitong-aiw.gree.com/openapi/v2/embeddings"
            )
        }
    }

    suspend fun updateModel(
        index: Int,
        alias: String?,
        apiKey: String?,
        modelName: String?,
        baseUrl: String?,
        chat: String?,
        embed: String?
    ): Boolean {
        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty() || chat.isNullOrEmpty() || modelName.isNullOrEmpty()) {
            ToastUtils.show("key/host/chat/modelName不能为空!")
            return false
        }
        _modelsObs[index].apply {
            this.alias = alias
            this.baseUrl = baseUrl
            this.apiKey = apiKey
            this.modelName = modelName
            this.chatCompletionPath = chat
            this.embeddingsPath = embed
        }
        saveString(DATA_MODEL_LIST, JsonApi.encodeToString(_modelsObs.toList()))
        return true
    }

    suspend fun addModel(
        alias: String?,
        apiKey: String?,
        modelName: String?,
        baseUrl: String?,
        chat: String?,
        embed: String?  //callback:(Boolean)->Unit
    ): Boolean {
        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty() || chat.isNullOrEmpty() || modelName.isNullOrEmpty()) {
            ToastUtils.show("key/host/chat/modelName不能为空!")
            return false
        }
        try {
            val modelInfoCell = ModelInfoCell(apiKey, modelName, baseUrl, chat, embed, alias)
            _modelsObs.add(0, modelInfoCell)
            saveString(DATA_MODEL_LIST, JsonApi.encodeToString(_modelsObs.toList()))
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun deleteModel(modelName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cache = getCacheString(DATA_MODEL_LIST)
            if (!cache.isNullOrEmpty()) {
                val list = JsonApi.decodeFromString<MutableList<ModelInfoCell>>(cache)
                _modelsObs.clear()
                if (list.isNotEmpty()) {
                    val iterator = list.iterator()
                    while (iterator.hasNext()) {
                        val tmp = iterator.next()
                        if (tmp.modelName == modelName) {
                            iterator.remove()
                            break
                        }
                    }
                    _modelsObs.addAll(list)
                    saveString(DATA_MODEL_LIST, JsonApi.encodeToString(list))
                } else {
                    removeCacheKey(DATA_MODEL_LIST)
                }
            }
        }
    }
}