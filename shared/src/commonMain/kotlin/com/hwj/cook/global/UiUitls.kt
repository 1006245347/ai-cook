package com.hwj.cook.global

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cook.shared.generated.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

/**
 * @author by jason-何伟杰，2025/2/28
 * des:识别 键盘按键
 */
@Composable
fun Modifier.KeyEventEnter(enter: () -> Unit, shift: () -> Unit): Modifier {
    return this.then( //合并之前的样式，不然会覆盖
        Modifier.onPreviewKeyEvent { event: KeyEvent ->
            when {
                event.key == Key.Enter && event.isShiftPressed
                        && event.type == KeyEventType.KeyDown -> {
                    shift()
                    false
                }

                event.key == Key.Enter
                        && event.type == KeyEventType.KeyDown && onlyDesktop() -> {
                    enter()
                    true
                }

                else -> false
            }
        })
}

//思考中动画
@Composable
fun LoadingThinking(text: String) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.DotLottie(
            archive = Res.readBytes("files/dotlottie/cloading.lottie")//37
        )
    }
    Row (modifier = Modifier.height(40.dp)){
        Text(
            text, color = MaterialTheme.colorScheme.onTertiary, fontSize = 13.sp,
            modifier = Modifier.padding(start = 18.dp, end = 0.dp, top = 6.dp, bottom = 6.dp)
        )
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever
            ),
            contentDescription = "thinking", modifier = Modifier.width(50.dp).fillMaxHeight()
                .align(Alignment.CenterVertically).padding(end=2.dp)
        )
    }
}


