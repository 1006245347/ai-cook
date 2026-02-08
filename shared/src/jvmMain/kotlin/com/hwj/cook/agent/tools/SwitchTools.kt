package com.hwj.cook.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.hwj.cook.models.Switch

/**
 * @author by jason-何伟杰，2025/12/8
 * des:ToolSet是jvm特有，支持注解显示说明，不然就用最基础的arg方式
 */
class SwitchTools(val switch: Switch) : ToolSet {

    @Tool
    @LLMDescription("Switches the state of the switch")
    fun switch(state: Boolean): String {
        switch.switch(state)
        return "Switched to ${if (state) "on" else "off"} "
    }

    @Tool
    @LLMDescription("Returns the state of the switch")
    fun switchState(): String {
        return "Switch is ${if (switch.isOn()) "on" else "off"}"
    }
}

//测试可以这样问：

// Tell me if the switch if on or off. Elaborate on how you will determine that. After that, if it was off, turn it on. Be very verbose in all the steps