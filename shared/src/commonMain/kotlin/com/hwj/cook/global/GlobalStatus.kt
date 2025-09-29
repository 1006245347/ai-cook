package com.hwj.cook.global

import com.hwj.cook.getPlatform


/**
 * @author by jason-何伟杰，2025/2/6
 * des:区分当前系统
 */
enum class OsStatus {
    ANDROID,
    IOS,
    WINDOWS,
    MACOS,
    LINUX,
    BROWSER,
    UNKNOWN
}

fun onlyMobile(): Boolean {
    return getPlatform().os == OsStatus.IOS
            || getPlatform().os == OsStatus.ANDROID
}

fun onlyDesktop(): Boolean {
    return getPlatform().os == OsStatus.WINDOWS
            || getPlatform().os == OsStatus.MACOS
            || getPlatform().os == OsStatus.LINUX
}