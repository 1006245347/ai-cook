package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.memory.config.MemoryScopeType
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.agents.memory.feature.nodes.nodeLoadFromMemory
import ai.koog.agents.memory.feature.nodes.nodeSaveToMemory
import ai.koog.agents.memory.model.Concept
import ai.koog.agents.memory.model.FactType
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.markdown.markdown
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.createMemoryProvider
import com.hwj.cook.agent.tools.ToolCI
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.getCacheString

/**
 * @author by jason-何伟杰，2025/10/13
 * des:在tech模块获取用户偏好
 */
class MemoryAgentProvider(
    override var title: String = "Chat",
    override val description: String = "A conversational agent that supports long-term memory, with clear and concise responses."
) : AgentProvider {

    override suspend fun provideAgent(
        onToolCallEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): AIAgent<String, String> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))

        val agentConfig = AIAgentConfig(prompt = prompt("prompt") {
            system("Hi,I'm a personal assistant.")
        }, model = OpenAIModels.Chat.GPT4o, maxAgentIterations = 50)


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

        val diagnosticResultsConcept = Concept(
            keyword = "diagnostic-results", description = """
            Information about diagnostic results for specific devices or error codes including:
            - Device identifiers and types
            - Error codes and descriptions
            - Diagnostic steps performed
            - Results and recommendations
            This information helps avoid repeating diagnostic steps for known issues.
        """.trimIndent(), factType = FactType.MULTIPLE
        )

        val organizationSolutionsConcept = Concept(
            keyword = "organization-solutions", description = """
            Information about solutions provided to corporate customers including:
            - Product or service involved
            - Issue description
            - Solution details
            - Customer organization
            This information helps in sharing knowledge across an organization.
        """.trimIndent(), factType = FactType.MULTIPLE
        )

        val strategy = strategy<String, String>(
            "memory-support", toolSelectionStrategy = ToolSelectionStrategy.NONE
        ) {

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

                val nodeLoadDiagnosticResults by nodeLoadFromMemory<String>(
                    concept = diagnosticResultsConcept,
                    subject = MemorySubjects.Machine,
                    scope = MemoryScopeType.PRODUCT
                )

                val nodeLoadOrganizationSolutions by nodeLoadFromMemory<String>(
                    concept = organizationSolutionsConcept,
                    subject = MemorySubjects.Organization,
                    scope = MemoryScopeType.PRODUCT
                )

                nodeStart then nodeLoadUserPreferences then nodeLoadUserIssues then nodeLoadDiagnosticResults then nodeLoadOrganizationSolutions then nodeFinish
            }

            //  推理过程用到的工具
            val listTools = listOf(ToolCI.UserInfoTool, ToolCI.DiagnosticTool)
            val supportSession by subgraphWithTask<String, String>(tools = listTools) { userInput ->
                markdown {
                    h2(
                        "You are a customer support agent that helps users resolve issues and tracks information for future reference"
                    )
                    text("You should:")
                    br()
                    bulleted {
                        item {
                            text(
                                "Understand the user's preferences and communication style. " + "Do not ask this explicitly, but use this information (if available) from your memory"
                            )
                        }
                        item { text("Review the user's issue history to provide context") }
                        item { text("Use diagnostic information to avoid repeating steps") }
                        item { text("Leverage organization-wide solutions when applicable") }
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

                val saveDiagnosticResults by nodeSaveToMemory<String>(
                    concept = diagnosticResultsConcept,
                    subject = MemorySubjects.Machine,
                    scope = MemoryScopeType.PRODUCT
                )

                val saveOrganizationSolutions by nodeSaveToMemory<String>(
                    concept = organizationSolutionsConcept,
                    subject = MemorySubjects.Organization,
                    scope = MemoryScopeType.PRODUCT
                )

                nodeStart then saveUserPreferences then saveUserIssues then saveDiagnosticResults then saveOrganizationSolutions then nodeFinish
            }

            //开始，获取历史回忆，模型使用逻辑推理，更新记忆，结束
            nodeStart then loadMemory then supportSession then saveToMemory then nodeFinish
        }

        val agent = AIAgent.Companion.invoke(
            promptExecutor = remoteAiExecutor, strategy = strategy, agentConfig = agentConfig
        ) {
            install(AgentMemory.Feature) {
                this.memoryProvider = createMemoryProvider()
                this.productName = DATA_APPLICATION_NAME //设置产品名，为了范围对应
            }
        }
        return agent
    }
}