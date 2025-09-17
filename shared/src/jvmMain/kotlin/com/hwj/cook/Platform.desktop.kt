package  com.hwj.cook

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.DarkColorScheme
import com.hwj.cook.global.LightColorScheme
import com.hwj.cook.global.OsStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class DesktopPlatform :Platform{
    override val name: String
        get() = "desktop> ${System.getProperty("os.name")}"
    override val os: OsStatus
        get() = checkSystem()

}

actual fun getPlatform():Platform = DesktopPlatform()

actual fun checkSystem(): OsStatus {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> OsStatus.MACOS
        os.contains("win") -> OsStatus.WINDOWS
        os.contains("nix") || os.contains("nux") || os.contains("ubu") -> OsStatus.LINUX
        else -> OsStatus.UNKNOWN
    }
}

@Composable
actual fun setColorScheme(isDark: Boolean): ColorScheme {
    return if (!isDark) {
        LightColorScheme
    } else {
        DarkColorScheme
    }
}

@Composable
actual fun createPermission(
    permission: PermissionPlatform,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
) {
    grantedAction()
}

actual fun createKtorHttpClient(timeout: Long?): HttpClient {
    return HttpClient {
        install(HttpTimeout) {
            timeout?.let {
//                requestTimeoutMillis = it //完整请求超时
                connectTimeoutMillis = timeout //建立链接
                socketTimeoutMillis = 5000
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true

            })
        }
//        install(SSE)
        install(Logging) {
//                level = LogLevel.ALL
//            level = LogLevel.INFO //接口日志屏蔽
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    }
}