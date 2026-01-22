package com.hwj.cook.global

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

//common
fun cBasic() = Color(0xFF014AA7)
fun cBasic4D014AA7() = Color(0x4D014AA7)
fun cLowOrange() = Color(0XFFFAAD96)
fun cTransparent() = Color(0x00000000)
fun cWhite() = Color(0xffffffff)
fun cRed() = Color(0xFFF25555)
fun cGreen() = Color(0xFF0FAA67)
fun cGreyF0F0F0() = Color(0xFFF0F0F0)
fun cGreyF6F6F6() = Color(0xFFF6F6F6)
fun cBlue629DE8() = Color(0xFF629DE8)
fun cBlue014AA7() = Color(0xFF014AA7)
fun cBlue244260FF() = Color(0x244260FF)

//background
fun cGreyEEEEEE() = Color(0xFFEEEEEE)
fun cHalfGrey80717171() = Color(0x80717171)
fun cHalfGrey80717172() = Color(0x30717171)

fun cDark99000000() = Color(0x99000000)
fun cHalf80565353() = Color(0x80565353)

//textColor
fun cGreyB5B5B5() = Color(0xFFB5B5B5)
fun cGrey333333() = Color(0xFF333333)
fun cGreyE4E4E4() = Color(0xFFE4E4E4)
fun cGrey666666() = Color(0xFF666666)
fun cGrey999999() = Color(0xFF999999)
fun cBlackTxt() = Color(0xFF000000)
fun cBlack333333() = Color(0xFF333333)
fun cBlue1A629DE8() = Color(0x1A629DE8) //1A629DE8
fun cBlueE658B2F6() = Color(0xE658B2F6)
fun cBlueFFF7FAFE() = Color(0xFFF7FAFE)

//去androidApp那里/res/values/colors.xml预览资源颜色
fun cDeepLine() = Color(0xFF3B3C3D)
fun cLightLine() = Color(0xA8AAAFFF)

@Composable
fun cAutoBg() = MaterialTheme.colorScheme.background

fun cAutoTxt(isDark: Boolean): Color {
    return if (isDark) isDarkTxt() else isLightTxt()
}

fun cSetBg(isDark: Boolean): Color {
    return if (isDark) {
        BackGroundColor2
    } else {
        BackGroundColor1
    }
}

val Purple80 = Color(0xFFD0BCFF)//粉
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4) //紫
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val PrimaryColor = Color(0xFF6650a4) //小蓝 0xFF2B4270    大蓝 2B62CA.

val BackGroundColor1 = Color(0xFFF9FBFF) //白 明亮模式
val BackGroundColor2 = Color(0xFF262828)//黑
val BackInnerColor1 = Color(0xFFFFFFFF)//白色
val BackInnerColor2 = Color(0xFF2C2E2F)//中黑 消息列表背景色
val BackTxtColor1 = Color(0xFF464F5C) //0xFF464F5C=浅黑

//val BackTxtColor2 = Color(0xFFDFDFDF) //0xFFDFDFDF=白
val BackTxtColor2 = Color(0xE9E8E8FF) //0xFFDFDFDF=白
val BackCodeTxtColor = Color(0xFF9798BA) //代码颜色
val BackCodeGroundColor = Color(0xFF292C33) //代码背景框
val BackHumanColor1 = Color(0XFFEAF1FC)
val BackHumanColor2 = Color(0xFF2B4270)


val BackGroundMessageHuman = Color(0xFFE2F0E9)
val BackGroundMessageGPT = Color(0xFF2F3237)
val ColorTextHuman = Color(0xFF3D3D4E)
val ColorTextGPT = Color(0xFFFFFFF2)
fun cOrangeFFB8664() = Color(0xFFFB8664)

//textSize
fun sp7() = 7.sp
fun sp8() = 8.sp
fun sp18() = 18.sp

//space

fun dp6() = 6.dp
fun dp10() = 10.dp