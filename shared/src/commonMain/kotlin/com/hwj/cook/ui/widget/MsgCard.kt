package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.agent.ChatMsg
import com.hwj.cook.global.BackCodeGroundColor
import com.hwj.cook.global.BackCodeTxtColor
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.ToastUtils
import com.hwj.cook.ui.viewmodel.ChatVm
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

/**
 * @author by jason-何伟杰，2026/1/13
 * des:问答下是一问一答布局
 */
@Composable
fun BotCommonCard(answer: String) {
    val subScope = rememberCoroutineScope()

    val state = rememberRichTextState()
    LaunchedEffect(Unit) {
        state.removeLink()
        state.config.codeSpanBackgroundColor = BackCodeGroundColor
        state.config.codeSpanColor = BackCodeTxtColor
//        chatViewModel.processGlobal(GlobalIntent.CheckDarkTheme)
        if (!state.isCodeSpan) {
            state.toggleCodeSpan()
        }
//        ```java //无法解析这个 只有  `Code span example` ,但是3点是代码块，一点是行内代码，
    }
    val answerState = remember { mutableStateOf("") }

    LaunchedEffect(answer) {
        subScope.launch(Dispatchers.Default) { //貌似频==================================
            // IO
            val newMsg = answer.replace("```java", "`")
                .replace("```", "`")
            answerState.value = newMsg
        }
    }

    RichText(
        state = state.apply {
            state.setMarkdown(answerState.value)
        },
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.onPrimary),
        color = MaterialTheme.colorScheme.onTertiary,
        style = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onTertiary
        )
    )
}


//最后一条消息显示是的功能菜单，复制，重新生成
@Composable
fun BotCommonMenu(msg : String){
    val chatVm = koinViewModel  (ChatVm::class)
    Row {
        IconButton(onClick = { //复制
            chatVm.copyToClipboard(msg)
            chatVm.viewModelScope.launch(Dispatchers.Main) {
                ToastUtils.show("复制成功")
            }
        }, modifier = Modifier.padding(start = 15.dp, end = 10.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = PrimaryColor, modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = { //重新生成
            chatVm.generateMsgAgain()
        }) {
            Icon(
                imageVector = Icons.Default.GeneratingTokens,
                contentDescription = "Generate Again",
                tint = PrimaryColor, modifier = Modifier.size(20.dp)
            )
        }
    }
}