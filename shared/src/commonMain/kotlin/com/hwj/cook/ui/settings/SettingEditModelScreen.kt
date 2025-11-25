package com.hwj.cook.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MapsUgc
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.except.ToolTipCase
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cTransparent
import com.hwj.cook.global.printE
import com.hwj.cook.models.ModelInfoCell
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.SettingVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

/**
 * @author by jason-何伟杰，2025/10/15
 * des:index是被+1的
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingEditModelScreen(navigator: Navigator, index: Int) {
    val mainVm = koinViewModel(MainVm::class)
    val settingVm = koinViewModel(SettingVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val modelsState = settingVm.modelsState.collectAsState().value
    val subScope = rememberCoroutineScope()
    val isAdd = index - 1 == -1
    var model: ModelInfoCell? = null
    if (!isAdd) {
        model = modelsState[index - 1]
    }

    val alias = remember { mutableStateOf(model?.alias ?: "") }
    val apiKey = remember { mutableStateOf(model?.apiKey ?: "") }
    val modelName = remember { mutableStateOf(model?.modelName ?: "") }
    val baseUrl = remember { mutableStateOf(model?.baseUrl ?: "") }
    val chatUrl = remember { mutableStateOf(model?.chatCompletionPath ?: "") }
    val embedUrl = remember { mutableStateOf(model?.embeddingsPath ?: "") }

    ThemeChatLite (){
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { //返回时上个页面被重载了
                        navigator.goBack()
                    }) {
                        Icon(Icons.Filled.ArrowBackIosNew, "back", tint = PrimaryColor)
                    }
                }, title = {
                    Text(text = if (isAdd) "新增大模型" else "更新大模型", color = cAutoTxt(isDark))
                }, colors = TopAppBarDefaults.topAppBarColors(
                    //smallTopAppBarColors
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = PrimaryColor,
                ), actions = {
                    if (!isAdd)
                        ToolTipCase(tip = "删除", content = {
                            IconButton(onClick = {
                                modelName.value.let {
                                    settingVm.deleteModel(it)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除", tint = PrimaryColor,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        })
                })

            HorizontalDivider(thickness = 8.dp, color = cTransparent())
            InputBox(alias, isDark, "模型别名如：deepseek-v3")
            InputBox(apiKey, isDark, "ApiKey")
            InputBox(modelName, isDark, "模型名如：deepseek-v3")
            InputBox(baseUrl, isDark, "域名如：https://baitong-it.com")
            InputBox(chatUrl, isDark, "模型对话接口：baitong/chat/completions")
            InputBox(
                embedUrl, isDark,
                "模型向量化（可空）：https://baitong-it.gree.com/openapi/v2/embeddings"
            )

            IconButton(
                onClick = {
                    subScope.launch(Dispatchers.Default) {
                        var flag: Boolean
                        if (isAdd) {
                            flag = settingVm.addModel(
                                alias.value,
                                apiKey.value,
                                modelName.value,
                                baseUrl.value,
                                chatUrl.value,
                                embedUrl.value
                            )
                        } else {
                            flag = settingVm.updateModel(
                                index - 1, alias.value,
                                apiKey.value,
                                modelName.value,
                                baseUrl.value,
                                chatUrl.value,
                                embedUrl.value
                            )
                        }
                        if (flag)
                            navigator.goBackWith(modelName.value)
                        //返回同时更新上个列表
                    }
                },//响应按钮事件
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally)
                    .size(30.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    tint = PrimaryColor
                )
            }
        }
    }
}

@Composable
private fun InputBox(inputState: MutableState<String>, isDark: Boolean, des: String) {
    OutlinedTextField(
        value = inputState.value,
        onValueChange = { newValue ->
            if (newValue.length < 300) {
                inputState.value = newValue
            } else {
                printE("too long!")
            }
        },
        modifier = Modifier.padding(start = 2.dp, top = 5.dp, end = 2.dp, bottom = 0.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp), maxLines = 3,
        placeholder = {
            Text(
                des,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                modifier = Modifier.background(cTransparent()).height(50.dp)
            )
        },
        textStyle = TextStyle(fontSize = 13.sp, color = cAutoTxt(isDark = isDark))
    )
}