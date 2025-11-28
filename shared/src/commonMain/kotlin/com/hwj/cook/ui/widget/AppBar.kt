package com.hwj.cook.ui.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.MapsUgc
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.hwj.cook.except.ToolTipCase
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cLowOrange
import com.hwj.cook.global.printD
import com.hwj.cook.global.urlToAvatarGPT
import com.hwj.cook.global.urlToImageAuthor
import com.hwj.cook.ui.viewmodel.ChatVm
import com.hwj.cook.ui.viewmodel.MainVm
import moe.tlaster.precompose.koin.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    pagerState: PagerState,
    onClickMenu: () -> Unit,
    onNewChat: () -> Unit,
    onShowNav: (Rect?) -> Unit
) {
    val mainVm = koinViewModel(MainVm::class)
    val chatVm = koinViewModel (ChatVm::class)
    var barBounds by remember { mutableStateOf<Rect?>(null) }
    val isAgentModelState by chatVm.isAgentModelState.collectAsState()
    var isAgentModel= if (isAgentModelState==0) false else true
    CenterAlignedTopAppBar(
        title = {
            val paddingSizeModifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                .size(32.dp)
            Box {
                    SlidingToggle(paddingSizeModifier, options = "Ask" to "Agent", selected = isAgentModel,
                        onSelectedChange = {
                            if (isAgentModel){

                            }else{

                            }
                        })
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
            if (pagerState.currentPage != pagerState.pageCount - 1)
                ToolTipCase(tip = "新建会话", content = {
                    IconButton(onClick = { onNewChat() }) {
                        Icon(
                            imageVector = Icons.Default.MapsUgc,
                            contentDescription = "新建会话", tint = PrimaryColor,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                })

            ToolTipCase(tip = "导航", content = {
                IconButton(onClick = { onShowNav(barBounds) }) { //要把参数传出去
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "导航", tint = PrimaryColor,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            })
        }, modifier = Modifier.onGloballyPositioned { layout ->
            val pos = layout.localToWindow(Offset.Zero)
            barBounds = Rect(
                left = pos.x,
                top = pos.y,
                right = pos.x + layout.size.width,
                bottom = pos.y + layout.size.height
            )
        }
    )
}

@Composable
fun SlidingToggle(
    modifier: Modifier = Modifier,
    options: Pair<String, String> = "Ask" to "Agent",
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(targetValue = if (selected) 80.dp else 0.dp)

    Box(
        modifier = modifier
            .width(160.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x33000000))
            .clickable {
                onSelectedChange(!selected)
            }
    ) {
        // 滑块
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(80.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF4CAF50))
        )

        // 左标签
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = options.first,
                color = if (!selected) Color.White else Color.Black
            )
        }

        // 右标签
        Box(
            modifier = Modifier
                .offset(x = 80.dp)
                .width(80.dp)
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = options.second,
                color = if (selected) Color.White else Color.Black
            )
        }
    }
}


@Composable
fun BoxScope.CenterTitle(modifier: Modifier){
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = rememberAsyncImagePainter(urlToAvatarGPT),//urlToAvatarGPT 好丑
            modifier = modifier.then(Modifier.clip(RoundedCornerShape(6.dp))),
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
