package di

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.provider.AICookAgentProvider
import com.hwj.cook.agent.provider.AgentProvider
import com.hwj.cook.agent.provider.CalculatorAgentProvider
import com.hwj.cook.agent.provider.ChatAgentProvider
import com.hwj.cook.agent.provider.McpSearchAgentProvider
import com.hwj.cook.agent.provider.MemoryAgentProvider
import com.hwj.cook.agent.provider.SuggestCookAgentProvider
import com.hwj.cook.createKtorHttpClient
import com.hwj.cook.data.local.SettingsFactory
import com.hwj.cook.data.repository.GlobalRepository
import com.hwj.cook.data.repository.SessionRepository
import com.hwj.cook.except.DataSettings
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString
import com.hwj.cook.ui.viewmodel.ChatVm
import com.hwj.cook.ui.viewmodel.CookVm
import com.hwj.cook.ui.viewmodel.MainVm
import com.hwj.cook.ui.viewmodel.SettingVm
import com.hwj.cook.ui.viewmodel.TechVm
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
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
    single { createKtorHttpClient(15000,{}) }

    single {
        val factory: SettingsFactory = get()
        factory.createSettings()
    }
    single { DataSettings() }

    single { GlobalRepository(get()) }
    single { SessionRepository() }
    //智能体单例
    factory<suspend () -> Pair<LLMClient, LLModel>> {
        {
            val apiKey = getCacheString(DATA_APP_TOKEN, "")
            Pair(OpenAiRemoteLLMClient(apiKey!!), OpenAIModels.Chat.GPT4o)
        }
    }
    single<AgentProvider<String, String>>(named("calculator")) {
        CalculatorAgentProvider(
            provideLLMClient = get()
        )
    }

    single<AgentProvider<String, String>>(named("cook")) { AICookAgentProvider() }
    single<AgentProvider<String, String>>(named("chat")) { ChatAgentProvider() }
    single<AgentProvider<String, String>>(named("search")) { McpSearchAgentProvider() }
    single<AgentProvider<String, String>>(named("memory")) { MemoryAgentProvider() }
//    single<AgentProvider<String, List<Message.Response>>(named("suggest")){ SuggestCookAgentProvider() }
//    single<AgentProvider<String, List<Message.Response>>(named("suggest")) { SuggestCookAgentProvider() }

    single<AgentProvider<String, String>>(named("suggest")) { SuggestCookAgentProvider() }
}

val modelModule = module { //viewModel一般用factory
//    factory { WelcomeScreenModel(get()) }
//    single { SettingsViewModel(get(), get(), get()) }
    single { MainVm(get(), get()) }
    single { SettingVm() }
    single { CookVm() }
    single { ChatVm(get(), get(),get()) }
    single { TechVm() }
}

/**
 * des:注意声明接口的完整路径，多个平台必须一致
 */
expect fun sharedPlatformModule(): Module