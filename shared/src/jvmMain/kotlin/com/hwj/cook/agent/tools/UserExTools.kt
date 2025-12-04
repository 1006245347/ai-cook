package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet


/**
 * @author by jason-何伟杰，2025/12/4
 * des:只处理jvm
 */
class UserExTools(private val showUserMsg: suspend (String) -> String) : ToolSet {

    @Tool
    @LLMDescription("Show user a message from the agent and wait for a response. Call this tool to ask the user something.")
    suspend fun showMsg(@LLMDescription("The message to show to the user.") msg: String): String {
        return showUserMsg(msg)
    }
}