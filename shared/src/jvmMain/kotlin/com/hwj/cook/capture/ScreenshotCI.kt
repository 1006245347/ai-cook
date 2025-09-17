package com.hwj.cook.capture


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

//UI 拖拽得到的坐标 * scaleFactor ≠ 实际屏幕坐标
//macOS 多屏/高DPI 下 Robot 截图区域
//效果suc,windows/macOS 主屏幕
class ScreenshotState11 {
    var startOffset by mutableStateOf(Offset.Zero)
    var endOffset by mutableStateOf(Offset.Zero)
    val selectionRect: Rect
        get() = Rect(startOffset, endOffset)
    var isSelecting by mutableStateOf(false)
}

val LocalMainWindow = staticCompositionLocalOf<ComposeWindow> { error("No Window provided") }

@Composable
fun ScreenshotOverlay(
    mainWindow: ComposeWindow,
    onCapture: (BufferedImage) -> Unit,
    onCancel: () -> Unit
) {
    var showActBtn by remember { mutableStateOf(false) }
    var capturedRect by remember { mutableStateOf<Rect?>(null) }
    val state = remember { ScreenshotState11() }
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowState = rememberWindowState(
        position = WindowPosition(0.dp, 0.dp),
        size = DpSize(screenSize.width.dp, screenSize.height.dp)
    )
    val subScope = rememberCoroutineScope()
    val focusReq = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        mainWindow.isVisible = false
    }

    Window(
        onCloseRequest = {
            mainWindow.isVisible = true

            onCancel()

        },
        state = windowState,
        transparent = true,
        undecorated = true,
        alwaysOnTop = true,
        focusable = true,
        resizable = false
    ) {
        LaunchedEffect(Unit) {
            window.requestFocus() //触发快捷键
            focusReq.requestFocus()
        }
        Box(modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusReq)
            .focusable()
            .pointerInput(Unit) { //识别鼠标右键取消
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.buttons.isSecondaryPressed
                        if (event.type == PointerEventType.Press && pressed) {
                            onCancel()
                            mainWindow.isVisible = true
                        }
                    }
                }
            }
            .onPreviewKeyEvent { keyEvent ->
//                printD("keyEvent>${keyEvent.key} ${Key.Escape}")
                if (keyEvent.key == Key.Escape) { //快捷键取消
                    onCancel()
                    mainWindow.isVisible = true
                    true
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        state.isSelecting = true
                        state.startOffset = offset
                        state.endOffset = offset
                    },
                    onDrag = { change, _ ->
                        state.endOffset = change.position
                    },
                    onDragEnd = {
                        capturedRect = state.selectionRect.normalize()
                        //如果只是点了两下，宽高都很少，不足以被认定为截图！
                        showActBtn = true
                    }
                )
            }
        ) {
//            println("canvas>${chatViewModel.isShotState.collectAsState().value}")
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 背景遮罩
                drawRect(Color.Black.copy(alpha = 0.3f))

                // 选区范围和尺寸
                if (state.isSelecting) {
                    val rect = state.selectionRect.normalize()
                    // 半透明填充
                    drawRect(
                        color = Color.White.copy(alpha = 0.05f),
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                    // 白色描边
                    drawRect(
                        color = Color.White,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            //在截图框下放按钮
            if (showActBtn && capturedRect != null) {
                val isFullCapture =
                    capturedRect!!.width > screenSize.width * 0.8f && capturedRect!!.height > screenSize.height * 0.8f
                val myModifier: Modifier
                if (isFullCapture) {
                    myModifier = Modifier.align(Alignment.TopEnd).padding(23.dp)
                } else {
                    val offx = with(LocalDensity.current) { capturedRect!!.right.toDp() - 180.dp }
                    val offy = with(LocalDensity.current) { capturedRect!!.bottom.toDp() + 0.dp }

                    myModifier = Modifier.offset(x = offx, y = offy)
                }
                Box(modifier = myModifier) {
                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(13.dp)) {
                        Button(onClick = {
                            showActBtn = false
                            mainWindow.isVisible = true
                            onCancel() //关闭
                        }) {
                            Text("取消", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(onClick = {
                            if (null != capturedRect) {
                                subScope.launch {
                                    captureSelectedArea(capturedRect!!) { pic ->
                                        //                                        val thumbnail =
//                                            BufferedImage(
//                                                200,
//                                                200,
//                                                BufferedImage.TYPE_INT_ARGB
//                                            ).apply {
//                                                createGraphics().drawImage(
//                                                    pic.getScaledInstance(
//                                                        200,
//                                                        200,
//                                                        java.awt.Image.SCALE_SMOOTH
//                                                    ),
//                                                    0, 0, null
//                                                )
//                                            }
//                                        onCapture(thumbnail) //传缩略图
                                        onCapture(pic)
                                    }
                                    withContext(Dispatchers.Main) {
                                        showActBtn = false
                                        mainWindow.isVisible = true
                                        onCancel()
                                    }
                                }
                            } else {
                                showActBtn = false
                                mainWindow.isVisible = true
                                onCancel()
                            }
                        }) {
                            Text("确定", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun captureSelectedArea(rect: Rect, onSuccess: (BufferedImage) -> Unit) {
    val normalizedRect = rect.normalize()

    val screenDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    var targetDevice: java.awt.GraphicsDevice? = null

//    // 找到选区落在哪块屏幕上
    for (device in screenDevices) {
        val bounds = device.defaultConfiguration.bounds
        if (bounds.contains(normalizedRect.left.toInt(), normalizedRect.top.toInt())) {
            targetDevice = device
            break
        }
    }

    //多屏不让用
//    if (screenDevices.size>1){
//        NotificationsManager().showNotification("不支持多屏截图！","不支持多屏截图")
//        return
//    }

//    targetDevice=screenDevices[0]

    if (targetDevice == null) {
        targetDevice =
            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    }

    val config = targetDevice!!.defaultConfiguration
    val screenBounds = config.bounds // 屏幕偏移（多屏布局下重要）
    val transform = config.defaultTransform
    val scaleX = transform.scaleX
    val scaleY = transform.scaleY
//    printD("屏幕 bounds: $screenBounds, scaleX: $scaleX, scaleY: $scaleY")

    // 关键：Compose 逻辑坐标 → 物理像素坐标 ,超级大坑，用chatgpt写代码一直反馈是乘scaleX,实际是除以，不然容易黑屏
    val captureX = (normalizedRect.left / scaleX).toInt()
    val captureY = (normalizedRect.top / scaleY).toInt()
    val captureW = (normalizedRect.width / scaleX).toInt()
    val captureH = (normalizedRect.height / scaleY).toInt()

//    printD("最终截图区域 (物理像素): x=$captureX, y=$captureY, w=$captureW, h=$captureH")

    if (captureW <= 0 || captureH <= 0) return

    try {
        // 隐藏截图窗口，防止蒙层被截进去
        val awtWindow = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
        awtWindow?.isVisible = false
        Thread.sleep(100) // 等隐藏生效

        val robot = Robot(targetDevice)
        val screenRect = Rectangle(captureX, captureY, captureW, captureH)
        val image = robot.createScreenCapture(screenRect)

        onSuccess(image)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun saveToFile(image: BufferedImage): String? {

//     val desktopPath = System.getProperty("user.home") + File.separator + "Desktop"
//     val file = File(desktopPath, "screenshot_${System.currentTimeMillis()}.png")

    val cacheDir = getPlatformCacheImgDir11()
    if (!cacheDir.exists()) cacheDir.mkdirs()

    val file = File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")

    ImageIO.write(image, "PNG", file)
//    printD("截图已保存到：${file.absolutePath}")
    ImageIO.write(image, "PNG", file).also {
        if (it) return file.absolutePath
    }
    return null
}

//截图已保存到缓存目录：/Users/你的用户名/Library/Caches/com.hwj.ai.capture/screenshot_1710918988888.png
//截图已保存到缓存目录：C:\Users\你的用户名\AppData\Local\com.hwj.ai.capture\cache\screenshot_1710918988888.png
//截图已保存到缓存目录：/home/你的用户名/.cache/com.hwj.ai.capture/screenshot_1710918988888.png
fun getPlatformCacheImgDir11(): File {
    val osName = System.getProperty("os.name").lowercase()
    return if (osName.contains("mac")) {
        File(System.getProperty("user.home"), "Library/Caches/com.hwj.ai.capture")
    } else if (osName.contains("win")) {
        File(System.getenv("LOCALAPPDATA"), "com.hwj.ai.capture/cache")
    } else {
        File(System.getProperty("user.home"), ".cache/com.hwj.ai.capture")
    }
}

/**
 * 扩展方法：统一 start/end，避免负数尺寸
 */
private fun Rect.normalize(): Rect {
    val left = minOf(this.left, this.right)
    val top = minOf(this.top, this.bottom)
    val right = maxOf(this.left, this.right)
    val bottom = maxOf(this.top, this.bottom)
    return Rect(left, top, right, bottom)
}