package com.hwj.cook.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MapsUgc
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.hwj.cook.except.ToolTipCase
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cLowOrange
import com.hwj.cook.global.urlToAvatarGPT
import com.hwj.cook.ui.viewmodel.MainVm
import moe.tlaster.precompose.koin.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(onClickMenu: () -> Unit, onNewChat: () -> Unit) {
    val mainVm = koinViewModel(MainVm::class)
    CenterAlignedTopAppBar(
        title = {
            val paddingSizeModifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                .size(32.dp)
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(urlToAvatarGPT),//urlToAvatarGPT 好丑
                        modifier = paddingSizeModifier.then(Modifier.clip(RoundedCornerShape(6.dp))),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI",
                        textAlign = TextAlign.Center,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        },
        navigationIcon = {
            ToolTipCase(tip = "边栏", content = {
                IconButton(
                    onClick = {
                        mainVm.collapsedDrawer()
                        onClickMenu()
                    },
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        "边栏",
                        modifier = Modifier.size(26.dp),
                        tint = PrimaryColor,
                    )
                }
            })
        },

        colors = TopAppBarDefaults.topAppBarColors(
            //smallTopAppBarColors
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = PrimaryColor,
        ),
        actions = {
            ToolTipCase(tip = "新建会话", content = {
                IconButton(onClick = { onNewChat() }) {
                    Icon(
                        imageVector = Icons.Default.MapsUgc,
                        contentDescription = "新建会话", tint = PrimaryColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            })
        }
    )
}


