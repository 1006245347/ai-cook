package com.hwj.cook.ui.tech

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoFloatBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cBasic
import com.hwj.cook.global.cBlackTxt
import com.hwj.cook.global.cDeepLine
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.global.cWhite
import com.hwj.cook.global.formatFileSize
import com.hwj.cook.global.mill2Date
import com.hwj.cook.global.truncate
import com.hwj.cook.models.FileInfoCell
import com.hwj.cook.runLiteWork
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.TechVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import org.example.dropdown.data.DropdownConfig
import org.example.dropdown.data.DropdownItemSeparator
import org.example.dropdown.data.ToggleIcon
import org.example.dropdown.data.enum.DefaultSelectorPosition
import org.example.dropdown.data.search.SearchSettings
import org.example.dropdown.data.selection.CheckboxParams
import org.example.dropdown.data.selection.MultipleItemContentConfig
import org.example.dropdown.ui.item.MultipleItemOptions
import org.example.project.ui.SearchableDropdown

/**
 * @author by jason-何伟杰，2026/1/28
 * des:RAG 单文件导入，向量化文件
 */
@Composable
fun RAGScreen() {
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val subScope = rememberCoroutineScope()
    val techVm = koinViewModel(TechVm::class)
    val fileInfoListState = techVm.fileInfoListState.collectAsState().value
    val selectedFileState = techVm.selectedFileState.collectAsState().value
    val deleteReq = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        subScope.launch(Dispatchers.Default) {
            techVm.initialize()
        }
    }

    LaunchedEffect(deleteReq.value) {
        if (deleteReq.value) {
            techVm.deleteRagFile()
            deleteReq.value = false
        }
    }
    SideEffect {

    }
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(top = 10.dp, end = 5.dp).border(
                width = 1.dp, color = cDeepLine(), shape = RoundedCornerShape(
                    topStart = 10.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp
                )
            ).padding(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(5.dp)) {
                Text(
                    text = "本地知识库",
                    color = cAutoTxt(isDark),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold //加粗
                )
                Text(
                    text = "添加文件>",
                    color = cAutoTxt(isDark),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp)
                        .clickable(onClick = {
                            subScope.launch { techVm.chooseFile() }
                        }).padding(3.dp)
                )
                Text(
                    text = "删除文件>${if (selectedFileState.isNotEmpty()) "${selectedFileState.size}" else ""}",
                    color = cAutoTxt(isDark),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable(onClick = {
                            subScope.launch {
                                deleteReq.value = true
//                                runLiteWork {  }
                            }
                        }).padding(3.dp)
                )
            }

            key(deleteReq.value) { //太辣鸡啦，好多bug 这控件
                MultipleCheckUI(
                    isDark = isDark,
                    deleteReq,
                    fileInfoListState,
                ) { isSelected, item ->
//                printD("select>$isSelected $item") //删除选中的
                    techVm.selectRagFile(isSelected, item)
                }
            }
        }
    }
}

//多选列表
@Composable
fun MultipleCheckUI(
    isDark: Boolean,
    deleteReq: MutableState<Boolean>,
    files: List<FileInfoCell>,
    callback: (Boolean, FileInfoCell) -> Unit
) {
    val firstItem = files.firstOrNull()
    val multipleConfig = MultipleItemContentConfig.Custom(
        content = { item, isSelected, selectListener -> //多选处理事件
            Column(Modifier.fillMaxWidth().height(if (firstItem == item) 55.dp else 45.dp)) {
                if (firstItem == item) {
                    Row {
                        Text(
                            text = "文件名称",
                            color = cAutoTxt(isDark),
                            fontSize = 10.sp,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "日期",
                            color = cAutoTxt(isDark),
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "大小",
                            color = cAutoTxt(isDark),
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "位置",
                            color = cAutoTxt(isDark),
                            fontSize = 10.sp,
                            modifier = Modifier.weight(3f)
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().height(50.dp).padding(8.dp)
                        .background(cAutoFloatBg(isDark)).clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isSelected) {
                                selectListener.onDeselect(item)
                            } else {
                                selectListener.onSelect(item)
                            }
                            callback(!isSelected, item)
                        }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected, onCheckedChange = { selected ->
                            when (selected) {
                                true -> selectListener.onSelect(item)
                                false -> selectListener.onDeselect(item)
                            }
                            callback(selected, item)
                        }, colors = CheckboxDefaults.colors(
                            checkedColor = cBasic(),
                            uncheckedColor = Color.Gray, checkmarkColor = cWhite()
                        ), modifier = Modifier.weight(0.6f)
                    )
                    Text(
                        text = item.name.truncate(15),//省略文字
                        color = cAutoTxt(isDark),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = mill2Date(item.millDate),
                        color = cAutoTxt(isDark),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.fileSize.formatFileSize(),
                        color = cAutoTxt(isDark),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.path.truncate(50),
                        color = cAutoTxt(isDark),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(3f)
                    )
                }
            }
        },
        header = { item, _, removeListener -> //得到一个项布局，组成一个横向列表
            Row(
                Modifier.fillMaxWidth().height(55.dp).padding(horizontal = 10.dp)
                    .background(cAutoFloatBg(isDark))
            ) {
                Text(text = item.name.truncate(15), color = cAutoTxt(isDark))
                Spacer(Modifier.width(2.dp))
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "",
                    tint = cAutoTxt(isDark),
                    modifier = Modifier.clickable {
                        removeListener.onRemove(item)
                        callback(false, item)
                    })
            }
        },
        options = MultipleItemOptions(
            selectionMaxCount = files.size,
            useDefaultSelector = false, //默认的checkbox没有对外响应接口，用不了
            defaultCheckboxParams = CheckboxParams(
                checkedColor = cAutoTxt(isDark), checkmarkColor = PrimaryColor
            ),
            defaultSelectorPosition = DefaultSelectorPosition.START //checkbox在前还是在后
        ),
    )

    SearchableDropdown(
        items = files,
        searchSettings = SearchSettings(searchEnabled = false),
        dropdownConfig = DropdownConfig(
            headerHeight = 55.dp,
            horizontalPadding = 8.dp,
            headerBackgroundColor = cAutoFloatBg(isDark, cBlackTxt()), //header颜色，点击弹列表
            contentBackgroundColor = cAutoFloatBg(isDark, cBlackTxt()), //整个背景颜色？
            headerPlaceholder = {
                Text(text = "RAG知识库文件", color = cAutoTxt(isDark))
            },
            maxHeight = 280.dp,
            separationSpace = -80, //影响弹出列表的padding
            toggleIcon = ToggleIcon(iconTintColor = cAutoTxt(isDark)),
            itemSeparator = DropdownItemSeparator(color = cAutoTxt(isDark)),
            shape = RoundedCornerShape(8.dp)
        ),
        itemContentConfig = multipleConfig
    )

}
