package com.hwj.cook.ui.tech

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.DATA_RAG_FILE
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoFloatBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cBlackTxt
import com.hwj.cook.global.formatFileSize
import com.hwj.cook.global.mill2Date
import com.hwj.cook.global.printD
import com.hwj.cook.global.removeCacheKey
import com.hwj.cook.global.truncate
import com.hwj.cook.models.FileInfoCell
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
            deleteReq.value = false
            techVm.deleteRagFile()
        }
    }
    Box(Modifier.fillMaxSize().padding(10.dp)) {
        MultipleCheckUI(
            isDark = isDark,
            deleteReq,
            fileInfoListState,
            selectedFileState
        ) { isSelected, item ->
            printD("select>$isSelected $item") //删除选中的
            if (isSelected) {
                techVm.selectRagFile(item.path)
            }
        }

        Button(
            onClick = {
                subScope.launch(Dispatchers.Default) { //打开文件管理器
                    techVm.chooseFile()
                }
            }, modifier = Modifier.align(alignment = Alignment.TopEnd).size(90.dp, 40.dp)
                .background(color = PrimaryColor)
        ) {
            Text(text = "添加文件", color = cAutoTxt(isDark), fontSize = 12.sp)
        }
    }
}

//多选列表
@Composable
fun MultipleCheckUI(
    isDark: Boolean,
    deleteReq: MutableState<Boolean>,
    files: List<FileInfoCell>,
    selectedFiles: List<FileInfoCell>?,
    callback: (Boolean, FileInfoCell) -> Unit
) {
    val firstItem = files.firstOrNull()
    val multipleConfig = MultipleItemContentConfig.Custom(
        content = { item, isSelected, selectListener -> //多选处理事件
            Column(Modifier.fillMaxWidth().height(if (firstItem == item) 90.dp else 50.dp)) {
                if (firstItem == item) {
                    Row {
                        if (!selectedFiles.isNullOrEmpty()) {
                            Button(onClick = {
                                deleteReq.value = true
                            }) {
                                Text(text = "删除", color = cAutoTxt(isDark), fontSize = 10.sp)
                            }
                        }
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
                    Modifier.fillMaxWidth().height(50.dp).padding(10.dp)
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
                        text = item.path.truncate(20),
                        color = cAutoTxt(isDark),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(3f)
                    )
                    if (isSelected) {
                        Image(
                            Icons.Default.Check,
                            contentDescription = "checked",
                            modifier = Modifier.size(25.dp)
                        )
                    }
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
            useDefaultSelector = true,
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
            headerBackgroundColor = cAutoFloatBg(isDark, cBlackTxt()), //header颜色，点击弹列表
            contentBackgroundColor = cAutoFloatBg(isDark, cBlackTxt()), //整个背景颜色？
            headerPlaceholder = {
                Text(text = "RAG知识库文件", color = cAutoTxt(isDark))
            },
            maxHeight = 280.dp,
            separationSpace = 0,
            toggleIcon = ToggleIcon(iconTintColor = cAutoTxt(isDark)),
            itemSeparator = DropdownItemSeparator(color = cAutoTxt(isDark)),
            shape = RoundedCornerShape(8.dp),
        ),
        itemContentConfig = multipleConfig
    )

}
