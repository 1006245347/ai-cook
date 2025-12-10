package com.hwj.cook.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.unit.dp
import com.hwj.cook.global.cBlue629DE8
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.models.ModelInfoCell
import moe.tlaster.precompose.navigation.Navigator

@Composable
fun SettingDefScreen(navigator: Navigator) {

    val cell = ModelInfoCell(
        "xx", "name1", "",
        "", "", "a1"
    )
    val list = mutableListOf<ModelInfoCell>()
    list.add(cell)
    list.add(cell)
    list.add(cell)
    list.addAll(list)

    Column {

        Spacer(Modifier.height(70.dp).background(cBlue629DE8()))

        SingleCheckUI(true, list, mutableStateOf(null), {
            navigator.goBack()
        })
    }
}