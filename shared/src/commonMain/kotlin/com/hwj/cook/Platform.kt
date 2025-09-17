package com.hwj.cook

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform