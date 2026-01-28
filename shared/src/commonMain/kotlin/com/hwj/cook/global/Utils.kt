@file:OptIn(ExperimentalTime::class, ExperimentalSettingsApi::class)

package com.hwj.cook.global

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.hwj.cook.except.DataSettings
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toBlockingSettings
import com.russhwolf.settings.serialization.decodeValueOrNull
import com.russhwolf.settings.serialization.encodeValue
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

//获取时间戳
fun getMills(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun getNowTime(): LocalDateTime {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}

//自定义格式化
fun LocalDateTime.formatDefault(): String{
    fun Int.pad2() = this.toString().padStart(2, '0')
    //pad2是补0
    return "${year}-${month.number.pad2()}-${day.pad2()} " +
            "${hour.pad2()}:${minute.pad2()}:${second.pad2()}"
}

fun today(): LocalDateTime {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
}

/**
 * Extension function to format file sizes.
 */
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return "${(size * 10).toInt() / 10.0} ${units[unitIndex]}"
}

/**
 * Extension function to truncate strings with ellipsis.
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else "${substring(0, maxLength - 3)}..."
}

internal const val openLog = true
const val logTAG = "COC"

//desktop端日志在 terminal显示
fun printD(log: String?, tag: String = logTAG) {
    if (!openLog) return
//    globalScope.launch {
        printLog(log, tag)
//    }
}

fun printD(log: String?, des: String? = null, tag: String = logTAG) {
    if (!openLog) return
//    globalScope.launch {
        printD("$des $log", tag)
//    }
}

fun initKermitLog() {
    Logger.setMinSeverity(Severity.Verbose)
    Logger.setTag(logTAG)
}

fun printLog(log: String?, tag: String = logTAG) {
    if (!openLog) return
    log?.let { Logger.d(tag) { it } }
}

fun printE(log: String?, tag: String = logTAG) {
    log?.let {
        globalScope.launch {
            Logger.e(tag) { it }
        }
    }
}

fun printE(throws: Throwable?, des: String? = null, tag: String = logTAG) {
    throws?.printStackTrace()
    throws?.let {
        globalScope.launch {
            Logger.e(tag) { throws.message+">$des"  }
        }
    }
}

fun printList(list: List<Any>?, des: String? = null, tag: String = logTAG) {
    des?.let { printD(">$des", tag) }
    if (list == null) {
        printD(">list is null", tag)
    } else {
        printD("printList-size>${list.size}")
        list.forEachIndexed { index, any -> printD("Item$index> $any", tag) }
    }
}

fun createImageName(): String {
    return "ai_${getMills()}.jpg"
}

fun ViewModel.workInSub(
    defaultDispatcher: CoroutineDispatcher =
        Dispatchers.Default, block: suspend CoroutineScope.() -> Unit
) {
    viewModelScope.launch(defaultDispatcher) {
        block()
    }
}

fun ViewModel.delayWork(
    delayMills: Long = 2000, defaultDispatcher: CoroutineDispatcher =
        Dispatchers.Default, block: suspend CoroutineScope.() -> Unit
) {
    workInSub(defaultDispatcher) {
        delay(delayMills)
        block()
    }
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun encodeImageToBase64(platformFile: PlatformFile): String {
    //readBytes内部有线程切换！
//    return "data:image/jpeg;base64," + Base64.encode(platformFile.readBytes())
    var code = ""
    runBlocking {
        val task = async {
            Base64.encode(platformFile.readBytes()) //貌似没啥用
        }
        code = task.await()
    }

    return "data:image/jpeg;base64,${code}"//.also { printD("pic> $it") }
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun encodeImageToBase64(byteArray: ByteArray): String {
    return "data:image/jpeg;base64," + Base64.encode(byteArray)
}


val globalScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
val settingsCache: FlowSettings = DataSettings().settingsCache

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> saveObj(key: String, value: T) {
    settingsCache.toBlockingSettings().encodeValue(key, value)
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> getCacheObj(key: String): T? {
    return settingsCache.toBlockingSettings().decodeValueOrNull<T>(key)
}

//注意，这种是开启新的线程异步执行，结果是线程安全
fun saveAsyncInt(key: String, value: Int) {
    //异步
    globalScope.launch {
        settingsCache.putInt(key, value)
    }
}

suspend fun saveInt(key: String, value: Int) {
    settingsCache.putInt(key, value)
}

fun saveAsyncString(key: String, value: String) {
    globalScope.launch {
        settingsCache.putString(key, value)
    }
}

suspend fun saveString(key: String, value: String) {
    settingsCache.putString(key, value)
}

fun saveAsyncBoolean(key: String, value: Boolean) {
    globalScope.launch {
        settingsCache.putBoolean(key, value)
    }
}

suspend fun saveBoolean(key: String, value: Boolean) {
    settingsCache.putBoolean(key, value)
}

fun saveAsyncFloat(key: String, value: Float) {
    globalScope.launch {
        settingsCache.putFloat(key, value)
    }
}

suspend fun saveFloat(key: String, value: Float) {
    settingsCache.putFloat(key, value)
}

fun saveAsyncDouble(key: String, value: Double) {
    globalScope.launch {
        settingsCache.putDouble(key, value)
    }
}

suspend fun saveDouble(key: String, value: Double) {
    settingsCache.putDouble(key, value)
}

suspend fun saveAsyncLong(key: String, value: Long) {
    globalScope.launch {
        settingsCache.putLong(key, value)
    }
}

suspend fun saveLong(key: String, value: Long) {
    settingsCache.putLong(key, value)
}


suspend fun getCacheInt(key: String, def: Int = 0): Int {
    return settingsCache.getInt(key, def)
}

suspend fun getCacheLong(key: String, def: Long = 0L): Long {
    return settingsCache.getLong(key, def)
}

suspend fun getCacheString(key: String): String? {
    return settingsCache.getStringOrNull(key)
}

suspend fun getCacheString(key: String, def: String): String? {
    return settingsCache.getString(key, def)
}

fun getAsyncString(key: String): Deferred<String?> {
    return globalScope.async { getCacheString(key) }
}

suspend fun getCacheBoolean(key: String, def: Boolean = false): Boolean {
    return settingsCache.getBoolean(key, def)
}

suspend fun getCacheFloat(key: String, def: Float = 0f): Float {
    return settingsCache.getFloat(key, def)
}

suspend fun getCacheDouble(key: String, def: Double = 0.0): Double {
    return settingsCache.getDouble(key, def)
}

suspend fun hasCacheKey(key: String): Boolean {
    return settingsCache.hasKey(key)
}

suspend fun removeCacheKey(key: String) {
    settingsCache.remove(key)
}

suspend fun clearCache() {
    settingsCache.clear()
}

