package com.hwj.cook.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.global.ToastUtils
import kotlinx.coroutines.delay

/**
 * @author by jason-何伟杰，2025/3/18
 * des:没反应
 */
@Composable
fun ToastHost(modifier: Modifier) {
    val messageState by ToastUtils.messageState.collectAsState()
    messageState?.let {
        LaunchedEffect(it) {
            delay(1500)
            ToastUtils.dismiss()
        }

        Box(
            modifier = modifier
        ) {
            Text(
                text = it, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 10.dp)
            )
        }
    }
}