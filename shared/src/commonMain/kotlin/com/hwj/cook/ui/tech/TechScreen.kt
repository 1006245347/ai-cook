package com.hwj.cook.ui.tech

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.dp10
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.TechVm
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

/**
 * @author by jason-何伟杰，2025/10/13
 * des:让用户将自身偏好、个人信息写出来，我们把它设置成记忆内容
 */
@Composable
fun TechScreen(navigator: Navigator) {
    val techVm = koinViewModel(TechVm::class)
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val uiObs by techVm.uiObs.collectAsState()
    val subScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        subScope.launch {
            techVm.loadInputCache()
            techVm.createAgent()
        }
    }

    TechScreenContent(
        isDark,
        inputTxt = uiObs.inputTxt,
        isInputEnabled = uiObs.isInputEnabled,
        isLoading = uiObs.isLoading,
        isInputEnded = uiObs.isInputEnded,
        memoryOfUser = uiObs.memoryOfUser,
        onInputTxtChanged = techVm::updateInputText,
        onSendClicked = techVm::sendFact2Memory
    )
}

@Composable
fun TechScreenContent(
    isDark: Boolean,
    inputTxt: String,
    isInputEnabled: Boolean,
    isLoading: Boolean,
    isInputEnded: Boolean,
    memoryOfUser: String?,
    onInputTxtChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Edit content for agent memory", fontSize = 20.sp, color = cAutoTxt(isDark)
            )

            OutlinedTextField(
                value = inputTxt,
                onValueChange = onInputTxtChanged,
                modifier = Modifier.padding(10.dp).fillMaxWidth()    //      .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Type some information...") },
                enabled = isInputEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClicked() }),
                singleLine = false,
                minLines = 5,
                maxLines = 10,
                shape = RoundedCornerShape(dp10())
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp).align(Alignment.CenterHorizontally),
                    color = Color.Yellow.copy(alpha = 0.7f),
                    strokeWidth = 8.dp,
                )
            } else {
                IconButton(
                    onClick = onSendClicked,//响应按钮事件
//                    enabled = isInputEnded && inputTxt.isNotBlank(),
                    modifier = Modifier.padding(top = 10.dp).size(30.dp).clip(CircleShape)
                        .background(
                            if (isInputEnabled && inputTxt.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (isInputEnabled && inputTxt.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            //大模型结果
            memoryOfUser?.let {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Text(text = it, fontSize = 11.sp, color = cAutoTxt(isDark))
                    }
                }
            }
        }
    }
}