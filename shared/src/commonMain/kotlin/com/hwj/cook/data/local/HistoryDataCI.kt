package com.hwj.cook.data.local

import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.agent.ChatSession
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.global.DATA_MESSAGE_TAG
import com.hwj.cook.global.DATA_SESSION_TAG
import com.hwj.cook.global.DATA_USER_ID
import com.hwj.cook.global.getCacheLong
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printList
import com.hwj.cook.global.removeCacheKey
import com.hwj.cook.global.saveString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @author by jason-何伟杰，2025/10/17
 * des:会话集合，会话内部不存消息，要找会话对应的消息列表则要有会话id
 */
suspend fun getSessionList(): MutableList<ChatSession>? {
    val cache = getCacheString(buildConversationTag())
    if (!cache.isNullOrEmpty()) {
        val list = JsonApi.decodeFromString<MutableList<ChatSession>>(cache)
        return list
    } else {
        return null
    }
}

suspend fun saveSession(conversation: ChatSession) {
    val cacheList = getSessionList()
    if (cacheList.isNullOrEmpty()) {
        val newList = mutableListOf<ChatSession>()
        newList.add(conversation)
        saveString(buildConversationTag(), JsonApi.encodeToString(newList))
    } else {
        cacheList.add(conversation)
        if (cacheList.size > 20) { //本地存储大小限制下
            cacheList.removeAt(0)
        }
        saveString(buildConversationTag(), JsonApi.encodeToString(cacheList))
    }
}

suspend fun saveSessionList(list: MutableList<ChatSession>?) {
    if (list.isNullOrEmpty()) {
        removeCacheKey(buildConversationTag())
    } else {
        saveString(buildConversationTag(), JsonApi.encodeToString(list))
    }
}

private suspend fun buildConversationTag(): String {
    return DATA_SESSION_TAG + getCacheLong(DATA_USER_ID)
}

suspend fun deleteMsgBySessionID(sessionId: String) {
    removeCacheKey(buildMsgTag(sessionId))
}

suspend fun getMsgList(sessionId: String): MutableList<ChatMsg>? {
    //缓存要跟userID绑定，不然切账号就乱了
    val result = getCacheString(buildMsgTag(sessionId))
    if (!result.isNullOrEmpty()) {
        val list = JsonApi.decodeFromString<MutableList<ChatMsg>>(result)
        return list
    } else {
        return null
    }
}

suspend fun saveMessage(message: ChatMsg) {
    val cacheList = getMsgList(message.sessionId)
    if (cacheList.isNullOrEmpty()) {
        val newList = mutableListOf<ChatMsg>()
        newList.add(message)
        saveString(buildMsgTag(message.sessionId), JsonApi.encodeToString(newList))
    } else {
        cacheList.add(0, message)
        saveString(buildMsgTag(message.sessionId), JsonApi.encodeToString(cacheList).also {
            printD(it, des = "saveMessage>")
        })
    }

}

suspend fun buildMsgTag(sessionId: String): String {
    return DATA_MESSAGE_TAG + getCacheLong(DATA_USER_ID) + "_$sessionId"
}

fun fetchMsgListFlow(sessionId: String): Flow<List<ChatMsg>> = callbackFlow {
    val list = getMsgList(sessionId)
    if (list.isNullOrEmpty()) {
        trySend(listOf())
    } else {
        trySend(list)
    }
    awaitClose { close() }
}

suspend fun fetchMsgList(sessionId: String): List<ChatMsg> {
    val list = getMsgList(sessionId)
    return if (list.isNullOrEmpty()) {
        listOf()
    } else {
        list
    }
}


//保存单条消息
suspend fun addMsg(chatMsg: ChatMsg): ChatMsg {
    saveMessage(chatMsg)
    return chatMsg
}

//有时候还没来得及保存数据，消息为空但是id相同
fun isNewSession(sessionId: String, sessionList: List<ChatSession>): Boolean {
    var isNew = true
    sessionList.forEach {
        if (it.id == sessionId) {
            isNew = false
        }
    }
    return isNew
}