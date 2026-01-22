package com.hwj.cook.global

import android.app.Application
import android.content.Context
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import di.initKoin
import org.koin.android.ext.koin.androidContext
import java.io.File

open class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this

        initKoin {
            //不加这个上下文，单例注入无法成功
            androidContext(androidContext = this@MainApplication)
        }

        initKermitLog()
        //采集用户信息的应在用户隐私同意后再初始化
        UmInitConfig.init()
    }

    companion object {
        lateinit var appContext: Context

        fun getUserCacheDir(): File? {
            return appContext.getExternalFilesDir("file_cache")
        }
    }
}