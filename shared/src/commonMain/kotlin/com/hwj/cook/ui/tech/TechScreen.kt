package com.hwj.cook.ui.tech

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
        subScope.launch { techVm.createAgent() }
    }

    TechScreenContent(
        isDark,
        inputTxt = uiObs.inputTxt,
        isInputEnabled = uiObs.isInputEnabled,
        isLoading = uiObs.isLoading,
        isInputEnded = uiObs.isInputEnded,
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
    onInputTxtChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {

    val focusRequester = remember { FocusRequester() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "Edit content for agent memory", fontSize = 20.sp, color = cAutoTxt(isDark))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(60.dp)
                            .align(Alignment.CenterHorizontally),
                    color = Color.Yellow.copy(alpha = 0.7f),
                    strokeWidth = 8.dp,
                )
            } else {
                Spacer(Modifier.height(60.dp))
            }

            OutlinedTextField(
                value = inputTxt,
                onValueChange = onInputTxtChanged,
                modifier = Modifier.padding(bottom = 100.dp)
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Type a message...") },
                enabled = isInputEnabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendClicked() }),
                singleLine = false,
                shape = RoundedCornerShape(dp10())
            )

        }
    }
}