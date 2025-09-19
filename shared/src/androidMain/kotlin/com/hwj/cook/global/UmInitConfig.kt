package com.hwj.cook.global

import com.umeng.commonsdk.UMConfigure
import com.umeng.umcrash.IUMCrashCallbackWithType
import com.umeng.umcrash.IUMCrashCallbackWithType.CrashType
import com.umeng.umcrash.UMCrash


object UmInitConfig {

    fun init() {
        //to259ygpaldtmnyvy0qeqj6dvyew0zse
        //d4a0647f4c1fcd9e2521109e66cd7a38
        UMConfigure.init(
            MainApplication.appContext,
            "68ccb976c261f2773323923b",
            "Umeng",
            UMConfigure.DEVICE_TYPE_PHONE,
            "d4a0647f4c1fcd9e2521109e66cd7a38"
        )
        UMConfigure.setLogEnabled(true)
//        UMCrash.registerUMCrashCallback(object : IUMCrashCallbackWithType {
//            override fun onCallback(type: CrashType): String? {
//                when (type) {
//                    CrashType.CRASH_TYPE_NATIVE -> return "Native 崩溃时register的自定义内容字符串"
//                    CrashType.CRASH_TYPE_JAVA -> return "JAVA 崩溃时register的自定义内容字符串"
//                    CrashType.CRASH_TYPE_ANR -> return "ANR 时register的自定义内容字符串"
//                    CrashType.CRASH_TYPE_CUSTOM_LOG -> return "自定义错误 register的自定义内容字符串"
//                    CrashType.CRASH_TYPE_BLOCK -> return "卡顿 时register的自定义内容字符串"
//                    else -> return null
//                }
//            }
//        })
    }

}