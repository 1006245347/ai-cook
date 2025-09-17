package com.hwj.cook.global

import android.app.Application
import android.content.Context
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
    }

    companion object {
        lateinit var appContext: Context

        fun getUserCacheDir(): File? {
            return appContext.getExternalFilesDir("file_cache")
        }
    }
}