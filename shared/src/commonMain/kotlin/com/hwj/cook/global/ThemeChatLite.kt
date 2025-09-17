package com.hwj.cook.global

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hwj.cook.setColorScheme
import kotlinx.coroutines.launch

/**
 * @author by jason-何伟杰，2025/2/25
 * des:不能随便引入主题，会重置主题色
 */
@Composable
fun ThemeChatLite(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    //兼容平台的主题色
    val colorScheme = setColorScheme(isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = ColorTextGPT
    ),

)

val DarkColorScheme = darkColorScheme( //在Android15没反应
    primary = PrimaryColor,
    secondary = Purple80, //大标题颜色
    tertiary = PrimaryColor,
    background = BackGroundColor2, //大背景
    surface = BackGroundColor2,
    onPrimary = BackInnerColor2, //聊天列表背景
    onSecondary = BackHumanColor2,//用户消息背景
    onTertiary = BackTxtColor2, //消息文字颜色 问答一致
    onPrimaryContainer = BackTxtColor2,
)

val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = Purple40,
    tertiary = PrimaryColor,
    background = BackGroundColor1,
    surface = BackGroundColor1,
    onPrimary = BackInnerColor1,
    onSecondary = BackHumanColor1,
    onTertiary = BackTxtColor1,
    onPrimaryContainer = BackTxtColor1,
)