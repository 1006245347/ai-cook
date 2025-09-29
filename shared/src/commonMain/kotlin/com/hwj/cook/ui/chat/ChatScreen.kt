package com.hwj.cook.ui.chat

import ai.koog.agents.core.agent.context.AIAgentLLMContext
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import com.hwj.cook.agent.AgentManager
import kotlinx.coroutines.launch
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun ChatScreen(globalNavigator: Navigator,insideNavigator: Navigator) {
    val subScope = rememberCoroutineScope()
    val sessionId = "ss"
    LaunchedEffect(sessionId) {
        subScope.launch {
//            AgentManager.quickAgent("你好")
        }

    }




}