package com.hwj.cook.ui.cook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hwj.cook.except.ToolTipCase
import com.hwj.cook.global.DATA_BOOK_ROOT
import com.hwj.cook.global.NavigationScene
import com.hwj.cook.global.PrimaryColor
import com.hwj.cook.global.cAutoTxt
import com.hwj.cook.global.cHalfGrey80717171
import com.hwj.cook.global.cOrangeFFB8664
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.loadingTip
import com.hwj.cook.global.printD
import com.hwj.cook.models.BookNode
import com.hwj.cook.ui.viewmodel.MainVm
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.encodeURLQueryComponent
import io.ktor.util.decodeBase64String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import moe.tlaster.precompose.koin.koinViewModel
import moe.tlaster.precompose.navigation.Navigator

/**
 * @author by jason-何伟杰，2025/9/30
 * des:菜谱文件预览
 */
@Composable
fun BookReadScreen(navigator: Navigator, encodePath: String) {
    val subScope = rememberCoroutineScope()
    var textState by remember { mutableStateOf<String?>(null) }
    val mainVm = koinViewModel(MainVm::class)
    val isDark = mainVm.darkState.collectAsState().value
    val realPath = encodePath.decodeURLQueryComponent()
    var tmpRoot by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(encodePath) {
        subScope.launch(Dispatchers.IO) {
            tmpRoot = getCacheString(DATA_BOOK_ROOT) + "/resource" // /Users/jasonmac/.aicook/files
            //根据路径读取内容
            textState = PlatformFile(realPath).readString()
            //dishes 、starsystem 、tips 同级，下面才是各类md文件
//            printD("real>$realPath") // /Users/jasonmac/.aicook/files/resource/starsystem/1Star.md
        }
    }
    val myUriHandler by remember {
        mutableStateOf(object : UriHandler {
            override fun openUri(uri: String) { //handle click
                //实际的 ./.. 等同于tmpRoot  /Users/jasonmac/.aicook/files/resource/
//                printD("uri>$uri") //  ./../dishes/breakfast/微波炉荷包蛋.md
                //https://github.com/Anduin2017/HowToCook/blob/master/starsystem/1Star.md?plain=1
                //https://github.com/Anduin2017/HowToCook/blob/master/dishes/breakfast/%E5%90%90%E5%8F%B8%E6%9E%9C%E9%85%B1.md

                if (uri.startsWith("./..")) {
                    //重新拼path,直接跳新page
                    val newPath = (tmpRoot + uri.replace(
                        "./..",
                        ""
                    )).encodeURLQueryComponent(encodeFull = true)
                    navigator.navigate(NavigationScene.BookRead.path + "/$newPath")
                }
            }
        })
    }

    val mdState = rememberRichTextState()
    Box(Modifier.fillMaxSize()) {
        if (textState == null) {
            Text(loadingTip, color = cAutoTxt(isDark))
        } else {
            Column(Modifier.padding(top = 16.dp).verticalScroll(rememberScrollState())) {
                CompositionLocalProvider(LocalUriHandler provides myUriHandler) {//增加link相应
                    RichText(
                        state = mdState.apply {
                            config.linkColor = PrimaryColor //要换
                            setMarkdown(textState!!)
                        }, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp).fillMaxSize()
                            .background(MaterialTheme.colorScheme.onPrimary),
                        color = cAutoTxt(isDark),
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = cAutoTxt(isDark)
                        )
                    )
                }
            }
        }

        Row(Modifier.padding(start = 8.dp, top = 5.dp).size(50.dp, 40.dp)) {
            ToolTipCase(tip = "返回", content = {
                IconButton(onClick = { navigator.goBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "返回", tint = PrimaryColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            })
        }
    }
}