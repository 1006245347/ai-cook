
package com.hwj.cook

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.memory.providers.AgentMemoryProvider
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import com.hwj.cook.agent.provider.AgentInfoCell
import com.hwj.cook.data.local.PermissionPlatform
import com.hwj.cook.global.OsStatus
import com.hwj.cook.models.BookNode
import com.hwj.cook.models.DeviceInfoCell
import io.ktor.client.HttpClient
import io.ktor.http.HeadersBuilder

interface Platform {

    val name: String
    val os: OsStatus
}

expect fun getPlatform(): Platform

expect fun createKtorHttpClient(timeout: Long?,builder: HeadersBuilder.() -> Unit): HttpClient

@Composable
expect fun setColorScheme(isDark: Boolean): ColorScheme

expect fun checkSystem(): OsStatus

//手机权限
@Composable
expect fun createPermission(
    vararg permissions: Any,
    grantedAction: () -> Unit,
    deniedAction: () -> Unit
)

expect fun listResourceFiles(path: String): BookNode?
expect fun readResourceFile(path: String): String?
expect fun loadZipRes(): String?

expect fun createFileMemoryProvider(scopeId: String=""): AgentMemoryProvider
expect fun getDeviceInfo(): DeviceInfoCell

//这样设计是为了保持使用jvm的ToolSet,却忽略iOS的ToolSet实现(根本就没有)
//这种做工具只能显示描述，不能用注解反射
expect interface PlatformToolSet
expect interface KmpToolSet: PlatformToolSet

//各个平台特属的工具
expect fun platformAgentTools(): ToolRegistry

//各个平台特属的智能体 累加
expect fun plusAgentList(): List<AgentInfoCell>

expect  suspend fun runLiteWork(call:()-> Unit)
