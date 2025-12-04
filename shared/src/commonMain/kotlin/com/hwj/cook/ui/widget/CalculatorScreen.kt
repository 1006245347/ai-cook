package com.hwj.cook.ui.widget

import androidx.compose.runtime.Composable
import com.hwj.cook.agent.provider.CalculatorAgentProvider
import org.koin.compose.getKoin
import org.koin.core.qualifier.named

/**
 * @author by jason-何伟杰，2025/12/4
 * des: 这里现在一种agent的构建
 */
@Composable
fun CalculatorScreen()
{

    val koin =getKoin()
    val agentProvider : CalculatorAgentProvider= koin.get(named("calculator"))
}