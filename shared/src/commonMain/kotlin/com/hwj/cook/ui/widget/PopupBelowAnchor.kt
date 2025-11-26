package com.hwj.cook.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hwj.cook.global.cHalfGrey80717172
import com.hwj.cook.global.cTransparent

/**
 * @author by jason-何伟杰，2025/11/14
 * des:根据其他控件位置显示的浮窗,bar右对齐
 */
@Composable
fun PopupBelowAnchor(anchor: Rect, onDismiss: () -> Unit, content: @Composable () -> Unit) {

    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectTapGestures { onDismiss() }
    }) {
        val density = LocalDensity.current
        var popWidth by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                popWidth = layoutCoordinates.size.width.toFloat()
            }.absoluteOffset(
                x = with(density) {
                    (anchor.right - popWidth).toDp()
                },
                y = with(density) { anchor.bottom.toDp() }
            ).background(cHalfGrey80717172())
        ) { content() }
    }
}