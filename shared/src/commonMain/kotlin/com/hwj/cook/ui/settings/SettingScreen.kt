package com.hwj.cook.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.DATA_MCP_KEY
import com.hwj.cook.global.NavigationScene
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cHalf80565353
import com.hwj.cook.global.cLightLine
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.global.dp10
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.saveAsyncString
import com.hwj.cook.global.saveString
import com.hwj.cook.models.ModelInfoCell
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.SettingVm
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator
import org.example.dropdown.data.DefaultDropdownItem
import org.example.dropdown.data.DropdownConfig
import org.example.dropdown.data.DropdownItemSeparator
import org.example.dropdown.data.ToggleIcon
import org.example.dropdown.data.listener.DropdownActionListener
import org.example.dropdown.data.search.SearchSettings
import org.example.dropdown.data.selection.SingleItemContentConfig
import org.example.project.ui.SearchableDropdown

@Composable
fun SettingScreen(navigator: Navigator) {
    val settingVm = koinViewModel(SettingVm::class)
    val mainVm = koinViewModel(MainVm::class)
    val modelsState by settingVm.modelsState.collectAsState()
    val isDark = mainVm.darkState.collectAsState().value
    val lastModel = remember { mutableStateOf<String?>(null) }
    val subScope = rememberCoroutineScope()

    SettingsScreenContent(modelsState, isDark, onAddClicked = { index ->
        subScope.launch { //跳转返回时携带参数
            val callback =
                navigator.navigateForResult(NavigationScene.SettingsEdit.path + "/$index")
                    .toString()
            lastModel.value = callback
            printD("lastModel>$callback")
        }
    }, onDefClicked = { model ->
        printD("defClick>$model")
        model?.let { settingVm.setDefModel(model) }
    }, onNavScreen = { navigator.navigate(NavigationScene.SettingsDef.path) })
}

@Composable
fun SettingsScreenContent(
    models: List<ModelInfoCell>,
    isDark: Boolean,
    onAddClicked: (Int) -> Unit,
    onDefClicked: suspend (ModelInfoCell?) -> Unit,
    onNavScreen: () -> Unit
) {
    var isShowDefView by remember { mutableStateOf(false) }
    var isShowMcpInput by remember { mutableStateOf(false) }
    var mcpKey by remember { mutableStateOf<String?>(null) }
    val subScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        subScope.launch {
            mcpKey = getCacheString(DATA_MCP_KEY)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.padding(5.dp).wrapContentWidth().align(Alignment.TopCenter),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(Modifier.padding(top = 10.dp, bottom = 10.dp, start = 5.dp)) {
                Text(
                    text = "新增大模型>",
                    fontSize = 15.sp,
                    color = PrimaryColor,
                    modifier = Modifier.weight(1f).clickable(onClick = { onAddClicked(0) })
                )

                Text(
                    text = "设置默认模型>",
                    fontSize = 15.sp,
                    color = PrimaryColor,
                    modifier = Modifier.weight(1f).clickable(onClick = {
                        isShowDefView = true
//                        onNavScreen()//测试 正确呀
                    })
                )

                Text(
                    text = "设置MCP KEY>",
                    fontSize = 15.sp,
                    color = PrimaryColor,
                    modifier = Modifier.weight(1f).clickable(onClick = { isShowMcpInput = true })
                )
            }
        }

        Card(
            modifier = Modifier.padding(top = 70.dp, start = 5.dp, end = 5.dp).fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(contentColor = cAutoBg())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                mcpKey?.let {
                    item {
                        Text(text = "mcp-key:$mcpKey", color = cAutoTxt(isDark))
                    }
                }

                itemsIndexed(models) { index, cell ->
                    val defLabel = if (cell.default) " 默认" else ""
                    Column(
                        Modifier.clickable(onClick = { onAddClicked(index + 1) })
                            .padding(top = 10.dp)
                    ) {

                        Text(
                            text = (cell.alias ?: "model$index") + defLabel,
                            fontSize = 13.sp,
                            color = cAutoTxt(isDark)
                        )
                        Text(text = cell.baseUrl, fontSize = 13.sp, color = cAutoTxt(isDark))
                        HorizontalDivider(
                            Modifier.padding(top = 3.dp), thickness = 1.dp, color = cLightLine()
                        )
                    }
                }
            }
        }

        if (isShowDefView) {
            Column(
                modifier = Modifier.padding(horizontal = 60.dp).fillMaxSize()
                    .clickable(onClick = { isShowDefView = false }), //背景取消){
            ) {
                Spacer(Modifier.height(70.dp).background(cOrangeFFB8664()))
                SingleCheckUI(
                    isDark, models, mutableStateOf(models.firstOrNull { it.default })
                ) { model -> //单选设为默认
                    subScope.launch {
                        onDefClicked(model)
                        isShowDefView = false
                    }
                }
            }
        }
        if (isShowMcpInput) {
            InputKey(isDark) { isShowMcpInput = false }
        }
    }
}

//单选列表
@Composable
fun SingleCheckUI(
    isDark: Boolean,
    models: List<ModelInfoCell>,
    selected: MutableState<ModelInfoCell?>,
    callback: (ModelInfoCell?) -> Unit
) {
    val singleConfig = SingleItemContentConfig.Default(
        defaultItem = DefaultDropdownItem(
            title = ModelInfoCell::modelName, subtitle = ModelInfoCell::alias, withIcon = false
        )
    )
    val list = mutableListOf<ModelInfoCell>() //测试
    list.addAll(models)
//    list.addAll(models)
//    list.addAll(models)
//    list.addAll(models)
//    list.addAll(models)
//    list.addAll(models)
//    list.addAll(models)

//    Column(
//        modifier = Modifier.padding(horizontal = 60.dp)
//            .clickable(onClick = { callback(null) }), //背景取消
////        contentAlignment = Alignment.TopCenter
//    ) {
//        //发现直接在父布局用padding top=70dp会导致分隔很远
//        Spacer(Modifier.height(70.dp).background(cBlue629DE8()))
    SearchableDropdown(
        items = list,
        searchSettings = SearchSettings(searchEnabled = false),
        dropdownConfig = DropdownConfig(
            headerHeight = 60.dp,
            headerBackgroundColor = cHalf80565353(),
            headerPlaceholder = {
                Text(
                    "默认大模型：${selected.value?.modelName ?: ""}",
                    color = cAutoTxt(isDark),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            },
            maxHeight = 280.dp,
            separationSpace = 10,
            toggleIcon = ToggleIcon(iconTintColor = cAutoTxt(isDark)),
            itemSeparator = DropdownItemSeparator(color = cAutoTxt(isDark)),
            shape = RoundedCornerShape(8.dp),
            dropdownActionListener = object : DropdownActionListener() {
                override fun <T> onItemSelect(item: T) {
//                    printD("click>$item")
                    callback(item as ModelInfoCell?)
                }
            }),
        selectedItem = selected,
        itemContentConfig = singleConfig
    )
//    }
}

@Composable
private fun InputKey(isDark: Boolean, callback: () -> Unit) {
    var inputTxt by remember { mutableStateOf("") }
    val colors = TextFieldDefaults.colors(
        focusedTextColor = cAutoTxt(isDark),
        unfocusedTextColor = cAutoTxt(isDark)
    )
    Column {
        OutlinedTextField(
            value = inputTxt,
            onValueChange = { inputTxt = it },
            modifier = Modifier.padding(10.dp).fillMaxWidth(),    //      .weight(1f) ,
            placeholder = { Text("Type some information...") },
            singleLine = false,
            minLines = 5,
            maxLines = 10,
            colors = colors,
            shape = RoundedCornerShape(dp10())
        )
        Button(onClick = {
            if (!inputTxt.isEmpty())
                saveAsyncString(DATA_MCP_KEY, inputTxt)
            callback.invoke()
        }) {
            Text(text = "保存", color = cAutoTxt(isDark))
        }
    }
}