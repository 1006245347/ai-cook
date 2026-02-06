package com.hwj.cook.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.except.BotMessageCard
import com.hwj.cook.except.BotMsgMenu
import com.hwj.cook.global.KeyEventEnter
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.dp10
import com.hwj.cook.global.dp6
import com.hwj.cook.global.thinkingTip
import com.hwj.cook.models.ModelInfoCell

/**
 * @author by jason-何伟杰，2025/10/10
 * des:对话页面的消息体UI
 */
@Composable
fun UserMessageBubble(isDark: Boolean, maxWidth: Dp, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(dp10()))
//                .background(MaterialTheme.colorScheme.primary)
                .padding(dp6())
        ) {
            SelectionContainer {
                Text(
                    text = text,
                    color = cAutoTxt(isDark), fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Composable
fun AgentMessageBubble(isDark: Boolean, maxWidth: Dp, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(dp10()))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(dp6())
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun SystemMessageItem(isDark: Boolean, maxWidth: Dp, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dp6()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ErrorMessageItem(isDark: Boolean, maxWidth: Dp, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
        ) {
            Text(
                text = "Error3",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = dp10())
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dp10()))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(dp6())
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ToolCallMessageItem(isDark: Boolean, maxWidth: Dp, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
        ) {
            Text(
                text = "Tool call",
                color = cAutoTxt(isDark),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = dp10())
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dp10()))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(dp6())
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun ResultMessageItem(
    isDark: Boolean,
    maxWidth: Dp,
    text: String,
    isLatest: Boolean,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = maxWidth)
        ) {
            Text(
                text = "Result",
                color = cAutoTxt(isDark),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = dp10())
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(dp10()))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(
                        start = 4.dp, end = 4.dp, top = 4.dp, bottom = if (isLatest) 10.dp else 4.dp
                    )
            ) {
                BotMessageCard(text)
            }
            if (!isLoading && isLatest&& text != thinkingTip) {
                BotMsgMenu(text)
            }
        }
    }
}

@Composable
fun RestartButton(isDark: Boolean, onRestartClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dp6(), vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onRestartClicked,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Start new chat")
        }
    }
}

@Composable
fun InputArea(
    isDark: Boolean,
    text: String,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onStopChat: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean, agentModel: ModelInfoCell?,
    focusRequester: FocusRequester
) {
    Box(
        modifier = Modifier.padding(horizontal = 4.dp).padding(top = 6.dp, bottom = 10.dp)
            .navigationBarsPadding().imePadding()
    ) {
        Row {
            TextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
//                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .focusRequester(focusRequester)
                    .weight(1f)
                    .KeyEventEnter(enter = {
                        onSendClicked()
                    }, shift = { //换行
                        val textClone = text + "\n"
                        onTextChanged(textClone)
                    }),
                placeholder = { Text("Type a message...") },
                enabled = isEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClicked() }),
                singleLine = false,
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(dp10())
            )

            EnterSendButton(
                isDark,
                isLoading,
                isEnabled && text.isNotBlank(),
                onSendClicked,
                onStopChat
            )
        }
    }
}

@Composable
private fun EnterSendButton(
    isDark: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    sendBlock: () -> Unit,
    stopBlock: () -> Unit
) {
    ExtendedFloatingActionButton(
        text = {
            Text(text = "Stop", color = Color.White)
        },
        icon = {
            if (isLoading) {
                Icon(
                    imageVector = Icons.Default.Stop, contentDescription = "Stop Generating",
                    tint = Color.White, modifier = Modifier.size(23.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }
        },
        onClick = {
            if (isLoading) {
                stopBlock()
            } else {
                if (enabled) {
                    sendBlock()
                }
            }
        },
        modifier = Modifier.animateContentSize().padding(start = 4.dp, end = 6.dp)
            .clip(CircleShape),
        expanded = isLoading,
        containerColor = PrimaryColor
    )

}


