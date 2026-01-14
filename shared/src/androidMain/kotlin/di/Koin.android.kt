package di

import com.hwj.cook.data.local.SettingsFactory
import com.hwj.cook.except.AndroidNetworkObserver
import com.hwj.cook.except.ClipboardHelper
import com.hwj.cook.except.NetworkObserver
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModule(): Module = module {
    single { SettingsFactory(context = androidContext()) }
    single <NetworkObserver>{ AndroidNetworkObserver(androidContext()) }
    factory { ClipboardHelper(context = get()) }
}