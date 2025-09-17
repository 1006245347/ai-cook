package di


import com.hwj.cook.data.local.SettingsFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun sharedPlatformModule(): Module = module {
    single { SettingsFactory() }

}