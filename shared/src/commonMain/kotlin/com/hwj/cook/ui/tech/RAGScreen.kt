package com.hwj.cook.ui.tech

import ai.koog.prompt.text.text
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoFloatBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cBlackTxt
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.global.printD
import com.hwj.cook.global.truncate
import com.hwj.cook.models.FileInfoCell
import com.hwj.cook.models.ModelInfoCell
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.TechVm
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import org.example.dropdown.data.DropdownConfig
import org.example.dropdown.data.DropdownItemSeparator
import org.example.dropdown.data.ToggleIcon
import org.example.dropdown.data.enum.DefaultSelectorPosition
import org.example.dropdown.data.listener.DropdownActionListener
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
    //记忆  rag
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val subScope = rememberCoroutineScope()
    val techVm = koinViewModel(TechVm::class)
    val fileInfoListState = techVm.fileInfoListState.collectAsState().value

    LaunchedEffect(Unit) {

    }



    Box(Modifier.fillMaxSize()) {
        MultipleCheckUI(isDark = isDark, fileInfoListState) { isSelected, item ->
            printD("i>$isSelected $item")
        }

        Button(onClick = {
            subScope.launch {
                techVm.chooseFile()
            }
        }, modifier = Modifier.align(alignment = Alignment.TopEnd)) {
            Text(text = "添加文件", color = cAutoTxt(isDark))
        }
    }

}

//多选列表
@Composable
fun MultipleCheckUI(
    isDark: Boolean, files: List<FileInfoCell>, callback: (Boolean, FileInfoCell) -> Unit
) {
    val firstItem = files[0]
    val multipleConfig = MultipleItemContentConfig.Custom(
        content = { item, isSelected, selectListener -> //多选处理事件
            Column(Modifier.fillMaxWidth().height(if (firstItem == item) 90.dp else 50.dp)) {
                if (firstItem == item) {
                    Row {
                        Text(
                            text = "文件名称",
                            color = cAutoTxt(isDark),
                            fontSize = 10.sp,
                            modifier = Modifier.weight(4f)
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
                            modifier = Modifier.weight(1f)
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
                        }) {
                    Text(
                        text = item.name.truncate(30),//省略文字
                        color = cAutoTxt(isDark),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
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
                Text(text = "文件-$item", color = cAutoTxt(isDark))
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
