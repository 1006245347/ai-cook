/*
 * Copyright 2023 Joel Kanyi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hwj.cook.global

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

sealed class UiEvents {
    data class ShowToast(val message: String) : UiEvents()
    data object Navigation : UiEvents()
    data object NavigateBack : UiEvents()
}

//Android15无法响应颜色切换
fun isDarkTxt(): Color { //onTertiary =
    return BackTxtColor2
}

fun isLightTxt(): Color {
    return BackTxtColor1
}

fun isDarkBg(): Color { //onSecondary
    return BackHumanColor2
}

fun isLightBg(): Color {
    return BackHumanColor1
}

fun isDarkPanel(): Color { //onPrimary
    return BackInnerColor2
}

fun isLightPanel(): Color {
    return BackInnerColor1
}

fun Modifier.roundBorderTextStyle(i: Int): Modifier {
    return Modifier.padding(end = 5.dp).background(
        color = cTransparent()
    ).border(
        width = 1.dp, color = cDeepLine(),
        shape = RoundedCornerShape(
            topStart = i.dp,
            topEnd = i.dp,
            bottomStart = i.dp,
            bottomEnd = i.dp
        )
    ).padding(horizontal = 6.dp, vertical = 3.dp)
}


