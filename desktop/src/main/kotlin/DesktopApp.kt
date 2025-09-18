import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.hwj.cook.PlatformWindowStart
import com.hwj.cook.global.initKermitLog
import di.initKoin
import org.koin.core.Koin


//编译运行命令 ./gradlew :desktop:run
//打包命令 打包时开翻墙要下编译库 ./gradlew packageDistributionForCurrentOS
//安装包路径 build/compose/binaries/main/
//build/compose/binaries/main/exe/
//build/compose/binaries/main/deb/
//Ubuntu/Debian: MyApp-1.0.0.deb

//control +  option +O       control + C 中断调试

lateinit var koin: Koin

fun main() {
    //依赖注入，不需要new对象，全模版生成。尽量放最前面
    koin = initKoin()
    koin.loadModules(
        listOf()
    )
    //日志
    initKermitLog() //这个方法是Utils.kt声明放在koin前面会报错

    return application {
        val windowState = rememberWindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 700.dp,
            height = 500.dp,
        )
        CompositionLocalProvider(
//            LocalAppResource provides rememberAppResource(),
        ) {
            val isShowWindowState = remember { mutableStateOf(true) } //控制主窗口显示
            val mainWindow = mutableStateOf<ComposeWindow?>(null)


            //正常启动
            PlatformWindowStart(windowState, isShowWindowState, onWindowChange = { w, isShow ->
                mainWindow.value = w
                //主要处理关闭窗口的响应，避免其他地方重复调用
                if (!isShow)
                    mainWindow.value?.isVisible = isShow //关闭主窗口
            }) {
                isShowWindowState.value = false
                exitApplication()
            }

        }
    }
}



