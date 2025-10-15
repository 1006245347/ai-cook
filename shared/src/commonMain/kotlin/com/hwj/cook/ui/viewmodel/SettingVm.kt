package com.hwj.cook.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.global.DATA_MODEL_LIST
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printLog
import com.hwj.cook.global.saveString
import com.hwj.cook.models.ModelInfoCell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

class SettingVm : ViewModel() {

    private val _modelsObs = mutableStateListOf<ModelInfoCell>()
    val modelsState = MutableStateFlow(_modelsObs).asStateFlow()

    fun initialize() {
        printLog("setInit>")
        viewModelScope.launch {
            val cache = getCacheString(DATA_MODEL_LIST)
            cache?.let {
                val list = JsonApi.decodeFromString<MutableList<ModelInfoCell>>(it)
                if (!list.isEmpty()) {
                    _modelsObs.clear()
                    _modelsObs.addAll(list)
                }
            }
        }
    }

    fun updateModel(
        index: Int,
        alias: String?,
        apiKey: String?,
        baseUrl: String?,
        chat: String?,
        embed: String?
    ) {
        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty() || chat.isNullOrEmpty()) {
            ToastUtils.show("key/host/chat不能为空!")
            return
        }
        _modelsObs[index].apply {
            this.alias = alias
            this.baseUrl = baseUrl
            this.apiKey = apiKey
            this.chatCompletionPath = chat
            this.embeddingsPath = embed
        }
    }

    fun addModel(alias: String?, apiKey: String?, baseUrl: String?, chat: String?, embed: String?) {
        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty() || chat.isNullOrEmpty()) {
            ToastUtils.show("key/host/chat不能为空!")
            return
        }
        val modelInfoCell = ModelInfoCell(apiKey, baseUrl, chat, embed, alias)
        _modelsObs.add(0, modelInfoCell)
        viewModelScope.launch {
            saveString(DATA_MODEL_LIST, JsonApi.encodeToString(_modelsObs))
        }
    }
}