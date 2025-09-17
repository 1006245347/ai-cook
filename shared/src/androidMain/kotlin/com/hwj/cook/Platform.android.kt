package com.hwj.cook

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import com.hwj.cook.global.askPermission
import com.hwj.cook.global.baseHostUrl
import com.hwj.cook.global.printD
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.gallery.GALLERY
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import dev.icerock.moko.permissions.storage.STORAGE
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val os: OsStatus
        get() = OsStatus.ANDROID
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun createKtorHttpClient(timeout: Long?): HttpClient {
    return HttpClient() {
        defaultRequest {
            url.takeFrom(URLBuilder().takeFrom(baseHostUrl))
        }

        install(HttpTimeout) {
            timeout?.let {
//                requestTimeoutMillis = timeout
                connectTimeoutMillis = timeout
                socketTimeoutMillis=5000
            }
        }
        install(SSE) {
            showCommentEvents()
            showRetryEvents()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(Logging) {
//            level = LogLevel.NONE
//            level = LogLevel.BODY
            level= LogLevel.INFO
//            level = LogLevel.NONE //接口日志屏蔽
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    printD(message, des = "http-android>")
                }
            }
        }
        //允许分块处理
        expectSuccess = true
    }
}

actual fun checkSystem(): OsStatus {
    return OsStatus.ANDROID
}

@Composable
actual fun setColorScheme(isDark: Boolean): ColorScheme {
    var colorScheme = LightColorScheme
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (isDark) {
            colorScheme = dynamicDarkColorScheme(context)
        } else {
            colorScheme = dynamicLightColorScheme(context)
        }
    } else {
        if (isDark) {
            colorScheme = DarkColorScheme
        } else {
            colorScheme = LightColorScheme
        }
    }

    //优化黑白主题下状态栏颜色
    val view = LocalView.current
    DisposableEffect(isDark) {
        val window = (view.context as ComponentActivity).window
        window.statusBarColor = colorScheme.background.toArgb()
        WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDark
        onDispose { }
    }
    return colorScheme
}

@Composable
actual fun createPermission(
    permission: PermissionPlatform,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    val p = when (permission) {
        PermissionPlatform.CAMERA -> Permission.CAMERA
        PermissionPlatform.GALLERY -> Permission.GALLERY
        PermissionPlatform.STORAGE -> Permission.STORAGE
        PermissionPlatform.WRITE_STORAGE -> Permission.WRITE_STORAGE
        PermissionPlatform.REMOTE_NOTIFICATION -> Permission.REMOTE_NOTIFICATION
        else -> Permission.STORAGE
    }
    askPermission(p, grantedAction, deniedAction)
}