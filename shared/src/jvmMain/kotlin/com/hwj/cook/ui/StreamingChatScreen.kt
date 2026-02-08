package com.hwj.cook.ui

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.streaming.StreamFrame
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.provider.chatAiAgent
import com.hwj.cook.agent.tools.SwitchTools
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.models.Switch
import kotlinx.coroutines.launch

/**
 * @author by jason-何伟杰，2026/1/23
 * des:测试流式工具，但崩溃啊
 */
@Composable
fun StreamingChatScreen() {
    val scope = rememberCoroutineScope()
    val inputState = remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<String>() }

    var agent by remember { mutableStateOf<AIAgent<String, *>?>(null) }
    val subScope = rememberCoroutineScope()

    // 处理事件回调
    LaunchedEffect(Unit) {
        subScope.launch {
            val switch = Switch()

            val toolRegistry = ToolRegistry {
                tools(SwitchTools(switch).asTools())
            }
            val apiKey = getCacheString(DATA_APP_TOKEN)!!
            val executor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey = apiKey))

            agent = chatAiAgent(toolRegistry, executor, {
                handleEvents {
                    onToolCallStarting { context ->
                        chatMessages += "🔧 使用工具 ${context.toolName} 参数: ${context.toolArgs}"
                    }
                    onLLMStreamingFrameReceived { context -> //流式返回？工具呢
                        (context.streamFrame as? StreamFrame.Append)?.let { frame ->
                            // 实时追加 LLM 输出
                            if (chatMessages.isEmpty() || chatMessages.last().startsWith("AI:")) {
                                chatMessages[chatMessages.lastIndex] += frame.text
                            } else {
                                chatMessages += "AI: ${frame.text}"
                            }
                        }
                    }
                    onLLMStreamingFailed {
                        chatMessages += "❌ LLM 错误: ${it.error}"
                    }
                    onLLMStreamingCompleted {
                        chatMessages += "✅ LLM 输出完成"
                    }
                }
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true // 最新消息在底部
        ) {
            items(chatMessages.reversed()) { msg ->
                Text(text = msg, modifier = Modifier.padding(4.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = inputState.value,
                onValueChange = { inputState.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = {
                val text = inputState.value
                if (text.isNotBlank()) {
                    chatMessages += "You: $text"
                    inputState.value = ""
                    scope.launch {
                        agent?.run(text)
                    }
                }
            }) {
                Text("发送")
            }
        }
    }
}