package com.hwj.cook.agent

import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.memory.config.MemoryScopeType
import ai.koog.agents.memory.feature.nodes.nodeLoadFromMemory
import ai.koog.agents.memory.feature.nodes.nodeSaveToMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.agents.memory.model.MemorySubject
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import com.hwj.cook.agent.tools.DiagnosticTool
import com.hwj.cook.agent.tools.UserInfoTool
import kotlinx.serialization.Serializable

object MemoryCI {
}

//定义事实 Memory concepts
val userPreferencesConcept = Concept(
    keyword = "user-preferences", description = """
            Information about the user's conversation preferences including:
            - Preferred lexicon and terminology
            - Preference for long or short responses
            - Communication style (formal, casual, technical)
            - Preferred contact methods
            This information helps in personalizing the support experience.
        """.trimIndent(), factType = FactType.MULTIPLE
)

val userIssuesConcept = Concept(
    keyword = "user-issues", description = """
            Information about the user's resolved and open issues including:
            - Issue descriptions and identifiers
            - Resolution status and details
            - Timestamps and duration
            - Related products or services
            This information helps in tracking the user's history with support.
        """.trimIndent(), factType = FactType.MULTIPLE
)

val deviceResultsConcept = Concept(
    keyword = "user-devices", description = """
            Information about  for user devices :
            - Device identifiers and types
            - Device operation system and descriptions
            This information useful sometime.
        """.trimIndent(), factType = FactType.MULTIPLE
)

val organizationSolutionsConcept = Concept(
    keyword = "organization-assignments", description = """
            Information about Work assignments for company departments including:
            - Department of the user's company
            - Work hours
            - The primary responsibilities, professional focus, and job position of the user
            - An overview of the current projects being undertaken by the user, including key project summaries, challenges encountered, and recommended solutions
            This information helps better connect the user's work-related details.
        """.trimIndent(), factType = FactType.MULTIPLE
)


//自定义记忆主题：
object MemorySubjects {

    /**
     * Information specific to the user
     * Examples: Conversation preferences, issue history, contact information
     */
    @Serializable
    data object User : MemorySubject() {
        override val name: String = "user"
        override val promptDescription: String =
            "User information (conversation preferences, issue history, contact details, etc.)"
        override val priorityLevel: Int = 1
    }

    /**
     * Information specific to the machine or device
     * Examples: Device type, error codes, diagnostic results
     */
    @Serializable
    data object Machine : MemorySubject() {
        override val name: String = "machine"
        override val promptDescription: String =
            "Machine or device information (device type, error codes, operation system, etc.)"
        override val priorityLevel: Int = 2
    }

    /**
     * Information specific to the organization
     * Examples: Corporate customer details, product information, solutions
     */
    @Serializable
    data object Organization : MemorySubject() {
        override val name: String = "organization"
        override val promptDescription: String =
            "Organization information (corporate customer details, product information, solutions, etc.)"
        override val priorityLevel: Int = 3
    }

    @Serializable
    data object Project : MemorySubject() {
        override val name: String = "project"
        override val promptDescription: String =
            "project information (project details, product information, solutions, etc.)"
        override val priorityLevel: Int = 3
    }
}

//涉及到的工具，深度封装？
private val memoryTools = listOf(UserInfoTool, DiagnosticTool)

//智能体的参数
val memoryAgentConfig = AIAgentConfig(
    prompt = prompt(
        id = "prompt",//toolChoice 大模型不认
        params = LLMParams(temperature = 0.8, toolChoice = null)
    ) {
        system("Hi,I'm a personal assistant.")
    }, model = buildQwen3LLM(), maxAgentIterations = 50
)

fun memoryStrategy() =
    strategy<String, String>(name = "memory", toolSelectionStrategy = ToolSelectionStrategy.NONE) {

        val loadMemory by subgraph<String, String>(tools = emptyList()) {
            val nodeLoadUserPreferences by nodeLoadFromMemory<String>(
                concept = userPreferencesConcept,
                subject = MemorySubjects.User,
                scope = MemoryScopeType.PRODUCT
            )

            val nodeLoadUserIssues by nodeLoadFromMemory<String>(
                concept = userIssuesConcept,
                subject = MemorySubjects.User,
                scope = MemoryScopeType.PRODUCT
            )

            val nodeLoadDeviceResults by nodeLoadFromMemory<String>(
                concept = deviceResultsConcept,
                subject = MemorySubjects.Machine,
                scope = MemoryScopeType.PRODUCT
            )

            val nodeLoadOrganizationSolutions by nodeLoadFromMemory<String>(
                concept = organizationSolutionsConcept,
                subject = MemorySubjects.Organization,
                scope = MemoryScopeType.PRODUCT
            )

            nodeStart then nodeLoadUserPreferences then nodeLoadUserIssues then nodeLoadDeviceResults then nodeLoadOrganizationSolutions then nodeFinish
        }

        val supportSession by subgraphWithTask<String, String>(tools = memoryTools) { userInput ->
            markdown {
                h2(
                    "You are a customer support agent that helps users resolve issues and tracks information for future reference."
                )
                text("You should:")
                br()
                bulleted {
                    item {
                        text(
                            "Understand the user's preferences and communication style. " +
                                    "Do not ask this explicitly, but use this information (if available) from your own knowledge or memory."
                        )
                    }
                    item { text("Review the user's issue history to provide context.") }
                    item { text("Use device information .") }
                    item { text("Leverage organization-wide solutions when applicable.") }
                    item { text("Solve the user's issue and provide a solution if possible") }
                }

                h2("User's question:")
                text(userInput)
            }
        }

        val saveToMemory by subgraph<String, String>(tools = emptyList()) {
            val saveUserPreferences by nodeSaveToMemory<String>(
                concept = userPreferencesConcept,
                subject = MemorySubjects.User,
                scope = MemoryScopeType.PRODUCT
            )

            val saveUserIssues by nodeSaveToMemory<String>(
                concept = userIssuesConcept,
                subject = MemorySubjects.User,
                scope = MemoryScopeType.PRODUCT
            )

            val saveDeviceResults by nodeSaveToMemory<String>(
                concept = deviceResultsConcept,
                subject = MemorySubjects.Machine,
                scope = MemoryScopeType.PRODUCT
            )

            val saveOrganizationSolutions by nodeSaveToMemory<String>(
                concept = organizationSolutionsConcept,
                subject = MemorySubjects.Organization,
                scope = MemoryScopeType.PRODUCT
            )

            nodeStart then saveUserPreferences then saveUserIssues then saveDeviceResults then
                    saveOrganizationSolutions then
                    nodeFinish
        }

        //开始，获取历史回忆，模型使用逻辑推理，更新记忆，结束
        nodeStart then loadMemory then supportSession then saveToMemory then nodeFinish

    }