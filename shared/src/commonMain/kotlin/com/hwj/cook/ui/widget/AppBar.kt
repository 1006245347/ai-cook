package com.hwj.cook.ui.widget

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.hwj.cook.global.DATA_AGENT_DEF
import com.hwj.cook.global.DATA_AGENT_INDEX
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cBlack333333
import com.hwj.cook.global.cBlackTxt
import com.hwj.cook.global.cBlue244260FF
import com.hwj.cook.global.cHalfGrey80717171
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.global.cWhite
import com.hwj.cook.global.getCacheInt
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.urlToAvatarGPT
import com.hwj.cook.ui.viewmodel.ChatVm
import com.hwj.cook.ui.viewmodel.MainVm
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    pagerState: PagerState,
    onClickMenu: () -> Unit,
    onNewChat: () -> Unit,
    onAgentPop: (Rect?) -> Unit,
    onShowNav: (Rect?) -> Unit
) {
    val mainVm = koinViewModel(MainVm::class)
    val chatVm = koinViewModel(ChatVm::class)
    val subScope = rememberCoroutineScope()
    var barBounds by remember { mutableStateOf<Rect?>(null) }
    val agentModelState by chatVm.agentModelState.collectAsState()
    var lastAgentState by remember { mutableStateOf<String>("") }
    //弹窗选择 智能体列表
    LaunchedEffect(Unit) {
        subScope.launch {
            lastAgentState = chatVm.getCacheAgent()
        }
    }

    CenterAlignedTopAppBar(
        title = {
            val paddingSizeModifier = Modifier
            Box {
                SlidingToggle(
                    paddingSizeModifier,
                    options = "Ask" to "Agent ${agentModelState ?: lastAgentState}",
                    selectedAgent = agentModelState != null,
                    onChangeAgent = {  //长按 弹窗
                        onAgentPop(barBounds)
                    },
                    onSelectedChange = { selected ->  //单击切换 问答/agent   //这是结果状态
                        subScope.launch {
                            if (selected) { //切到agent模式，那么就是ask切过来的，找ask上次缓存的模式
                                chatVm.changeChatModel(getCacheString(DATA_AGENT_DEF, "cook"))
                            } else {
                                chatVm.changeChatModel(null)
                            }
                            lastAgentState = chatVm.getCacheAgent()
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
                    IconButton(onClick = {
//                        onNewChat()
                        //测试
                        chatVm.test()
                    }) {
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
    selectedAgent: Boolean, onChangeAgent: () -> Unit,
    onSelectedChange: (Boolean) -> Unit
) {
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val thumbOffset by animateDpAsState(targetValue = if (selectedAgent) 81.dp else 1.dp)
    val cBottom = if (isDark) cHalfGrey80717171() else Color(0xFFE7EAEF) //全部底色 22262C  FF22282F
    val cTop = if (isDark) cBlackTxt() else cWhite() //滑块颜色
    val cTxt = if (isDark) cWhite() else cBlackTxt()

    Box(
        modifier = modifier
            .width(162.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cBottom)
            .combinedClickable(
                onLongClickLabel = "长按切换Agent",
                onClickLabel = "点击切换问答模式",
                onLongClick = {
                    onChangeAgent()
                },
                onClick = {
                    onSelectedChange(!selectedAgent)
                    printD("clickToggle? $selectedAgent")
                }
            )
    ) {
        // 滑块周边留1dp
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 1.dp)
                .width(80.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(cTop)
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
                color = cTxt,
                fontSize = if (!selectedAgent) 14.sp else 15.sp
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
                color = cTxt,
                fontSize = if (!selectedAgent) 10.sp else 10.sp
            )
        }
    }
}


@Composable
fun BoxScope.CenterTitle(modifier: Modifier) {
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
