package com.hwj.cook.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.NavigationScene
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoBg
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cLightLine
import com.hwj.cook.models.ModelInfoCell
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.SettingVm
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun SettingScreen(navigator: Navigator) {
    val settingVm = koinViewModel(SettingVm::class)
    val mainVm = koinViewModel(MainVm::class)
    val modelsState by settingVm.modelsState.collectAsState()
    val isDark = mainVm.darkState.collectAsState().value
    SettingsScreenContent(modelsState, isDark, onAddClicked = { index ->
        navigator.navigate(NavigationScene.SettingsEdit.path + "/$index")
    })
}

@Composable
fun SettingsScreenContent(
    models: List<ModelInfoCell>,
    isDark: Boolean,
    onAddClicked: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (models.isEmpty()) {
            Card(
                modifier = Modifier.padding(top=30.dp).wrapContentWidth()
                    .clickable(onClick = { onAddClicked(0) }),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "新增大模型",
                    fontSize = 15.sp,
                    color = PrimaryColor,
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )
            }


        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(contentColor = cAutoBg())
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    itemsIndexed(models) { index, cell ->
                        Column(
                            Modifier.clickable(onClick = { onAddClicked(index + 1) })
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = cell.alias ?: "model$index",
                                fontSize = 13.sp,
                                color = cAutoTxt(isDark)
                            )
                            Text(text = cell.baseUrl, fontSize = 13.sp, color = cAutoTxt(isDark))
                            HorizontalDivider(
                                Modifier.padding(top = 3.dp),
                                thickness = 1.dp,
                                color = cLightLine()
                            )
                        }
                    }
                }
            }
        }
    }
}