package com.hwj.cook.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet

class GlobalRepository(private val client : HttpClient) {

    suspend fun isConnectNet(): Boolean {
        var isSuc = false
        try {
            client.prepareGet("https://www.baidu.com") {
                timeout { requestTimeoutMillis = 3000 }
            }.execute { response ->
                if (response.status.value == 200) {
                    isSuc = true
                }
            }
            return isSuc
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}