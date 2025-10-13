package com.hwj.cook

import ai.koog.agents.memory.providers.AgentMemoryProvider
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.OsStatus
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import io.ktor.client.HttpClient

interface Platform {

    val name: String
    val os: OsStatus
}

expect fun getPlatform(): Platform

expect fun createKtorHttpClient(timeout: Long?): HttpClient

@Composable
expect fun setColorScheme(isDark: Boolean): ColorScheme


expect fun checkSystem(): OsStatus

//手机权限
@Composable
expect fun createPermission(
    permission: PermissionPlatform,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
)

expect fun listResourceFiles(path: String): BookNode?
expect fun readResourceFile(path: String): String?
expect fun loadZipRes(): String?

expect fun createFileMemoryProvider(scopeId: String=""): AgentMemoryProvider
expect fun getDeviceInfo(): DeviceInfoCell

fun getPermissionManager(){}



