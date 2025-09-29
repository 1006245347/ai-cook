package di


import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.data.local.SettingsFactory
import com.hwj.cook.data.repository.ConversationRepository
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.except.DataSettings
import com.hwj.cook.ui.viewmodel.ConversationViewModel
import com.hwj.cook.ui.viewmodel.CookVm
import com.hwj.cook.ui.viewmodel.MainVm
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * @author by jason-何伟杰，2025/2/12
 * des: 依赖注入 https://www.jianshu.com/p/bccb93a78cee
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(
        mainModule, modelModule, sharedPlatformModule()
    )
}

@Suppress("unused") // currently only used in debug builds,for ios
fun initKoin(): Koin {
    return initKoin { }.koin
}

//依赖注入目的是为了对象创建解耦，对象不在new具体的类，而是根据模版依赖生成
//factory每次都会创建新实例，而single是单例
val mainModule = module {
    single { createKtorHttpClient(15000) }

    single {
        val factory: SettingsFactory = get()
        factory.createSettings()
    }
    single { DataSettings() }

    single { GlobalRepository(get()) }
    single { ConversationRepository() }
}

val modelModule = module {
//    factory { WelcomeScreenModel(get()) }
//    single { SettingsViewModel(get(), get(), get()) }
    single { MainVm(get(),get()) }
    single { ConversationViewModel(get()) }
    single { CookVm() }
}

/**
 * des:注意声明接口的完整路径，多个平台必须一致
 */
expect fun sharedPlatformModule(): Module