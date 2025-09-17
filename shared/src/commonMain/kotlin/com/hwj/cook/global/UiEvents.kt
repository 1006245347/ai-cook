/*
 * Copyright 2023 Joel Kanyi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hwj.cook.global

import androidx.compose.ui.graphics.Color
import com.aallam.openai.api.chat.ChatChunk
import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.Tool
import com.aallam.openai.api.chat.ToolCall
import com.aallam.openai.api.core.Parameters
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

sealed class UiEvents {
    data class ShowToast(val message: String) : UiEvents()
    data object Navigation : UiEvents()
    data object NavigateBack : UiEvents()
}

//Android15无法响应颜色切换
fun isDarkTxt(): Color { //onTertiary =
    return BackTxtColor2
}

fun isLightTxt(): Color {
    return BackTxtColor1
}

fun isDarkBg(): Color { //onSecondary
    return BackHumanColor2
}

fun isLightBg(): Color {
    return BackHumanColor1
}

fun isDarkPanel(): Color { //onPrimary
    return BackInnerColor2
}

fun isLightPanel(): Color {
    return BackInnerColor1
}


//function call 是指模型在回答用户问题时，调用外部函数或API来获得信息或与外部系统交互的能力。
fun ToolCall.Function.execute(): String {
    val functionToCall =
        availableFunctions[function.name] ?: error("Function ${function.name} not found")
    val functionArgs = function.argumentsAsJson()
    return functionToCall(functionArgs)
}

val availableFunctions = mapOf("currentWeather" to ::callCurrentWeather)

/**
 * Example of a fake function for retrieving weather information based on location and temperature unit.
 * In a production scenario, this function could be replaced with an actual backend or external API call.
 */
fun callCurrentWeather(args: JsonObject): String {
    return when (val location = args.getValue("location").jsonPrimitive.content) {
        "San Francisco" -> """"{"location": "San Francisco", "temperature": "72", "unit": "fahrenheit"}"""
        "San Francisco, CA" -> """"{"location": "San Francisco", "temperature": "72", "unit": "fahrenheit"}"""
        "Tokyo" -> """{"location": "Tokyo", "temperature": "10", "unit": "celsius"}"""
        "Paris" -> """{"location": "Paris", "temperature": "22", "unit": "celsius"}"""
        else -> """{"location": "$location", "temperature": "unknown", "unit": "unknown"}"""
    }
}

/**
 * Appends a chat message to a list of chat messages.
 */
fun MutableList<ChatMessage>.append(message: ChatMessage) {
    add(
        ChatMessage(
            role = message.role,
            content = message.content.orEmpty(),
            toolCalls = message.toolCalls,
            toolCallId = message.toolCallId,
        )
    )
}

/**
 * Appends a function call and response to a list of chat messages.
 */
fun MutableList<ChatMessage>.append(toolCall: ToolCall.Function, functionResponse: String) {
    val message = ChatMessage(
        role = ChatRole.Tool,
        toolCallId = toolCall.id,
        name = toolCall.function.name,
        content = functionResponse
    )
    add(message)
}

val toolWeather = Tool.function(
    name = "currentWeather",
    description = "Get the current weather in a given location",
    parameters = Parameters.buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("location") {
                put("type", "string")
                put("description", "The city and state, e.g. San Francisco, CA")
            }
            putJsonObject("unit") {
                put("type", "string")
                putJsonArray("enum") {
                    add("celsius")
                    add("fahrenheit")
                }
            }
        }
        putJsonArray("required") {
            add("location")
        }
    }
)




