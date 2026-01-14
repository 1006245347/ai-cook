package di


import com.hwj.cook.data.local.SettingsFactory
import com.hwj.cook.except.ClipboardHelper
import com.hwj.cook.except.IOSNetworkObserver
import com.hwj.cook.except.NetworkObserver
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModule(): Module = module {
    single { SettingsFactory() }
    single<NetworkObserver> { IOSNetworkObserver() }
    factory { ClipboardHelper() }
}