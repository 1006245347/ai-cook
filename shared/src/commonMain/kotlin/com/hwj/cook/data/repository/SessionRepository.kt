package com.hwj.cook.data.repository

import com.hwj.cook.agent.ChatSession
import com.hwj.cook.data.local.deleteMsgBySessionID
import com.hwj.cook.data.local.getSessionList
import com.hwj.cook.data.local.saveSession
import com.hwj.cook.data.local.saveSessionList

/**
 * @author by jason-何伟杰，2025/9/18
 * des:使用koog的会话历史记录
 */
class SessionRepository {

    suspend fun fetchSession(): MutableList<ChatSession> {
        val list = getSessionList()
        return if (list.isNullOrEmpty()) {
            mutableListOf()
        } else {
            list.reversed().toMutableList()
        }
    }

    suspend fun addSession(session: ChatSession): ChatSession {
        saveSession(session)
        return session
    }

    //外部刷新
    suspend fun deleteSession(sessionId: String, onFinished: () -> Unit) {
        val list = getSessionList()
        if (!list.isNullOrEmpty()) {
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val tmp = iterator.next()
                if (tmp.id == sessionId) {
                    //要同时删除这个id下的所有消息记录
                    deleteMsgBySessionID(sessionId)
                    iterator.remove()
                    break
                }
            }
        }
        saveSessionList(list)
        onFinished()  //通知外部刷新
    }
}