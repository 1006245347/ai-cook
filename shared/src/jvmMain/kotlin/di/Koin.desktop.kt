package di

import com.hwj.cook.data.local.SettingsFactory
import com.hwj.cook.except.JvmNetworkObserver
import com.hwj.cook.except.NetworkObserver
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModule(): Module = module {
    single { SettingsFactory() }
    single<NetworkObserver> { JvmNetworkObserver() }

    //koog好多预设工具都是针对jvm的，导致iOS、kmp不适用

}