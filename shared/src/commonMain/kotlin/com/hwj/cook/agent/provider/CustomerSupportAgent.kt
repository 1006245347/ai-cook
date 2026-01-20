package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.agents.memory.providers.AgentMemoryProvider
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import kotlinx.serialization.Serializable

fun createMemoryAgent(
    memoryProvider: AgentMemoryProvider,
    promptExecutor: PromptExecutor,
    maxAgentIterations: Int = 50,
    productName: String? = null,
): AIAgent<String, String> {
    // Memory concepts
    //这些事实定义是给agent在运行时自行决定存储的事实，而我打算手动存，让agent去调用记忆的文件
    val userPreferencesConcept = Concept(
        keyword = "user-preferences",
        description = """
            Information about the user's conversation preferences including:
            - Preferred lexicon and terminology
            - Preference for long or short responses
            - Communication style (formal, casual, technical)
            - Preferred contact methods
            This information helps in personalizing the support experience.
        """.trimIndent(),
        factType = FactType.MULTIPLE
    )

    val userIssuesConcept = Concept(
        keyword = "user-issues",
        description = """
            Information about the user's resolved and open issues including:
            - Issue descriptions and identifiers
            - Resolution status and details
            - Timestamps and duration
            - Related products or services
            This information helps in tracking the user's history with support.
        """.trimIndent(),
        factType = FactType.MULTIPLE
    )

    val diagnosticResultsConcept = Concept( //这里例子检测设备硬件参数了。
        keyword = "diagnostic-results",
        description = """
            Information about diagnostic results for specific devices or error codes including:
            - Device identifiers and types
            - Error codes and descriptions
            - Diagnostic steps performed
            - Results and recommendations
            This information helps avoid repeating diagnostic steps for known issues.
        """.trimIndent(),
        factType = FactType.MULTIPLE
    )

    val organizationSolutionsConcept = Concept(
        keyword = "organization-solutions",
        description = """
            Information about solutions provided to corporate customers including:
            - Product or service involved
            - Issue description
            - Solution details
            - Customer organization
            This information helps in sharing knowledge across an organization.
        """.trimIndent(),
        factType = FactType.MULTIPLE
    )

    // Agent configuration
    val agentConfig = AIAgentConfig(
        prompt = prompt("customer-support") {},
        model = OpenAIModels.Chat.GPT4o,
        maxAgentIterations = maxAgentIterations
    )

//    val memoryProvider= LocalFileMemoryProvider()
    return AIAgent.Companion.invoke(
        promptExecutor = promptExecutor,
        agentConfig = agentConfig
    )
}