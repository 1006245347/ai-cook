package com.hwj.cook.agent.provider

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.dsl.extension.onMultipleToolResults
import ai.koog.agents.core.dsl.extension.onToolNotCalled
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.memory.feature.AgentMemory
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponses
import com.hwj.cook.agent.JsonApi
import com.hwj.cook.agent.OpenAiRemoteLLMClient
import com.hwj.cook.agent.buildEditLLM
import com.hwj.cook.agent.buildQwen3LLM
import com.hwj.cook.agent.buildQwenLLMClient
import com.hwj.cook.agent.createMemoryProvider
import com.hwj.cook.agent.tools.RecipeSearchTool
import com.hwj.cook.agent.tools.RecipeTools
import com.hwj.cook.global.DATA_APPLICATION_NAME
import com.hwj.cook.global.DATA_APP_TOKEN
import com.hwj.cook.global.DATA_MODEL_DEF
import com.hwj.cook.global.getCacheObj
import com.hwj.cook.global.getCacheString
import com.hwj.cook.global.printD
import com.hwj.cook.global.printLog
import com.hwj.cook.models.ModelInfoCell
import kotlinx.coroutines.flow.toList

/**
 * @author by jason-何伟杰，2025/10/10
 * des: Agent for cook
 *
 * 理解下 单次运行，koog的设计都是单次允许agent,避免重复的上下文和状态被多次加入调用，
 * 它的开发者有提到 AIAgentService来处理多次会话
 */
//class AICookAgentProvider : AgentProvider<String, String> {
class AICookAgentProvider : AgentProvider<String, List<Message.Response>> {
    override var title: String = "Chef Agent"

    override val description: String = """
                   I'm a professional chef skilled in various cooking techniques. If the user's inquiry isn't related to cooking, I'll still provide a brief response. All replies will be clear and concise.
                    Your goal is to recommend recipes from the local knowledge base.
                    - When the user asks for something to eat, use tools to search recipes.
                    """.trimIndent()


    override suspend fun provideAgent(
        prompt: Prompt,
        onToolCallEvent: suspend (Message.Tool.Call) -> Unit,
        onToolResultEvent: suspend (Message.Tool.Result) -> Unit,
        onLLMStreamFrameEvent: suspend (String) -> Unit,
        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String
    ): GraphAIAgent<String, List<Message.Response>> {
        val apiKey = getCacheString(DATA_APP_TOKEN)
        require(apiKey?.isNotEmpty() == true) { "apiKey is not configured." }
//        val remoteAiExecutor = SingleLLMPromptExecutor(OpenAiRemoteLLMClient(apiKey))
        val remoteAiExecutor = SingleLLMPromptExecutor(buildQwenLLMClient(apiKey))
        //获取当前默认的大模型
//        val modelInfo = getCacheObj<ModelInfoCell>(DATA_MODEL_DEF)

        val toolRegistry = ToolRegistry {
            tool(RecipeSearchTool)
        }

        //非流式
//        val strategy = fastStrategy(onAssistantMessage = onAssistantMessage)
        val strategy = streamingStrategy2(onAssistantMessage)

        val agentConfig = AIAgentConfig(
            prompt = prompt, model = buildQwen3LLM("Qwen/Qwen2.5-7B-Instruct"),
            maxAgentIterations = 30
        )

//        val userMemoryAgentProvider = createMemoryProvider()
        val agent = AIAgent.invoke(
            promptExecutor = remoteAiExecutor,
            strategy = strategy, agentConfig = agentConfig, toolRegistry = toolRegistry
        ) {

//            install(AgentMemory) {
//                this.memoryProvider = userMemoryAgentProvider
//                this.productName = DATA_APPLICATION_NAME //设置产品名，为了范围对应
//            }
            handleEvents {
                onToolCallStarting { ctx ->
//                    onToolCallEvent("\n🔧 Using ${ctx.toolName} with ${ctx.toolArgs}... ")
                    onToolCallEvent(
                        Message.Tool.Call(
                            id = ctx.toolCallId,
                            tool = ctx.toolName,
                            part = ContentPart.Text(text = JsonApi.encodeToString(ctx.toolArgs)),
                            metaInfo = ResponseMetaInfo.Empty //对不上类型
                        )
                    )
                }
                onToolCallCompleted { ctx ->
                    onToolResultEvent(
                        Message.Tool.Result(
                            id = ctx.toolCallId,
                            tool = ctx.toolName,
                            part = ContentPart.Text(text = JsonApi.encodeToString(ctx.toolArgs)),
                            metaInfo = RequestMetaInfo.Empty
                        )
                    )
                }
                onLLMStreamingFrameReceived { context ->
                    when (val chunk = context.streamFrame) {
                        is StreamFrame.Append -> {
                            onLLMStreamFrameEvent(chunk.text)
                        }

                        is StreamFrame.ToolCall -> {

                        }

                        is StreamFrame.End -> {
                            printLog("\n[END] reason=${chunk.finishReason}")
                        }
                    }
                    //可细分帧类型数据
//                    (context.streamFrame as? StreamFrame.Append)?.let { frame ->
////                        print(frame.text)
//                        onLLMStreamFrameEvent(frame.text)
//                    }
                }
                onLLMStreamingFailed {
//                    println("❌ Error: ${it.error}")
                    onErrorEvent("❌ Error: ${it.error}")
                }

                onAgentExecutionFailed { ctx -> //这个会返回给外部
                    onErrorEvent("failed1>${ctx.throwable.message}")
                }

                onAgentCompleted { ctx ->
                    // Skip finish event handling
//                    printD("agentCompleted>${ctx.result}")
                }
            }
        }
        return agent
    }

    fun streamingStrategy() = strategy("rag-search") {

        //发送LLM请求（可生成多个工具调用或回复）<String, List<Message.Response>> LLM分析用户问题，决定是否调用工具
        val nodeRequestLLM by nodeLLMRequestMultiple(name = "planner-llm")
        //执行工具
        val executeTools by nodeExecuteMultipleTools()
        //LLM流式请求 <T, List<Message.Response>>
        val nodeStreaming by nodeLLMRequestStreamingAndSendResults()

        //用户input转为模型数据List<Message.Request>
        val mapStringToRequests by node<String, List<Message.Request>> { input ->
            listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
        }

//        val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input ->
//            llm.writeSession {
//                appendPrompt {
//                    input.filterIsInstance<Message.User>()
//                        .forEach { user(it.content) }
//                    tool {
//                        input.filterIsInstance<Message.Tool.Result>()
//                            .forEach { result(it) }
//                    }
//                }
//                //上面塞入上下文，input是用户说的话
//                input
//            }
//        }
        val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input: List<Message.Request> ->
            llm.writeSession {
                appendPrompt {
                    input.filterIsInstance<Message.User>()
                        .forEach { user(it.content) }
                    tool {
                        input.filterIsInstance<Message.Tool.Result>()
                            .forEach { result(it) }
                    }
                }
                //上面塞入上下文，input是用户说的话
                input
            }
        }


        //将tool执行结果转为LLM理解的Message
        val mapToolCallsToRequests by node<List<ReceivedToolResult>, List<Message.Request>> { input ->
            input.map { it.toMessage() }
        }

        val filterValidTool by node<List<Message>, List<Message>> { messages ->
            messages.filter {
                when (it) {
                    is Message.Tool.Call -> it.id != null
                    else -> true
                }
            }
        }

//        val detectExit by node<String, Boolean> { input ->
//            input.trim().lowercase() in listOf("bye", "quit", "end", "结束", "再见")
//        }

        edge(nodeStart forwardTo mapStringToRequests)
        edge(mapStringToRequests forwardTo applyRequestToSession)
        edge(applyRequestToSession forwardTo nodeStreaming)

        edge(
            //当LLM想工具执行时，执行nodeStreaming流向 executeTools
            nodeStreaming forwardTo executeTools
                    //聚合所有tool后，但是也存在没聚合成功啊，
                    onMultipleToolCalls { toolCalls ->
                toolCalls.isNotEmpty() && toolCalls.all { it.isReady() }
            }
        )


        //工具执行 执行循环
        edge(executeTools forwardTo mapToolCallsToRequests)
        //工具结果写入上下文
        edge(mapToolCallsToRequests forwardTo applyRequestToSession)

        //如果LLM的输出没有工具调用，说明它说完了 ，这个靠LLM决定
//        edge(
//            nodeStreaming forwardTo nodeFinish
//                    onCondition { it.filterIsInstance<Message.Tool.Call>().isEmpty() }
//        )

        edge(
            nodeStreaming forwardTo nodeFinish
                    onCondition { requests ->
                requests.any {
                    it is Message.User && isExitCmd(it.content)
                }

            }
        )


    }

    fun Message.Tool.Call.isReady(): Boolean {
        if (tool.isBlank()) return false
        if (parts.isEmpty()) return false

        val argsText = parts.joinToString("") { it.text }.trim()

        // arguments 还没拼完
        if (!argsText.startsWith("{")) return false

        // 尝试解析 JSON（最稳）
        return runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(argsText)
        }.isSuccess
    }

    fun isExitCmd(content: String): Boolean {
        return (content.contains("bye")
                || content.contains("quit")
                || content.contains("end")
                || content.contains("结束")
                || content.contains("再见"))
    }

    fun streamingStrategy2(onAssistantMessage: suspend (String) -> String) =
        strategy("rag-search") {
            val nodeRequestLLM by nodeLLMRequestMultiple(name = "planner-llm")
            val executeTools by nodeExecuteMultipleTools()

            val nodeStreaming by nodeLLMRequestStreamingAndSendResults()

            val applyRequestToSession by node<List<Message.Request>, List<Message.Request>> { input: List<Message.Request> ->
                llm.writeSession {
                    appendPrompt {
                        input.filterIsInstance<Message.User>()
                            .forEach { user(it.content) }
                        tool {
                            input.filterIsInstance<Message.Tool.Result>()
                                .forEach { result(it) }
                        }
                    }
                    input
                }
            }

            //用户input转为模型数据List<Message.Request>
            val mapStringToRequests by node<String, List<Message.Request>> { input ->
                listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
            }
            val mapToolCallsToRequests by node<List<ReceivedToolResult>, List<Message.Request>> { input ->
                input.map { it.toMessage() }
            }
            // 输出LLM生成的消息到UI 回调 onAssistantMessage 没有工具走这分支
//        val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }
            val nodeAssistantMessage by node<String, List<Message.Response>> { input ->

                listOf(Message.Assistant(content = input, metaInfo = ResponseMetaInfo.Empty))
            }


            edge(nodeStart forwardTo nodeRequestLLM)
            edge(
                nodeRequestLLM forwardTo executeTools
                        onMultipleToolCalls { true } //怎么决定要用这判断？
            )
            edge( //适配不调用tool直接返回的情况
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            edge(executeTools forwardTo mapToolCallsToRequests)

            edge(mapToolCallsToRequests forwardTo applyRequestToSession)
            //applyRequestToSession用户说的话 + tools
            edge(applyRequestToSession forwardTo nodeStreaming)


            //流式输出、用户没有主动end ，基本都是false，不满足条件那么这条边其实就是无效不存在
//            edge(
//                nodeStreaming forwardTo nodeFinish
//                        onCondition { requests ->
//                            requests.any {
//                        it is Message.User && isExitCmd(it.content)
//                    }.also { printD("flag>$it") }
//
//                }
//            )

            //没有工具是不会执行这边的
//            edge(nodeStreaming forwardTo  executeTools onMultipleToolCalls {
//                true
//            })

//            edge(nodeStreaming forwardTo nodeFinish onCondition { requests->
//                requests.any{ it is Message.User&& isExitCmd(it.content)}
//            })
//            edge ( nodeStreaming forwardTo nodeFinish transformed { messages->
//                messages
////                messages.firstOrNull()?.content?:listOf(Message.Response)
//            })
            edge(nodeStreaming forwardTo nodeFinish)

            edge(nodeAssistantMessage forwardTo nodeFinish onCondition { true })
        }

    fun fastStrategy(onAssistantMessage: suspend (String) -> String): AIAgentGraphStrategy<String, String> {
        return strategy<String, String>("rag-search") {
            //llm负责决定工具使用
            val nodeRequestLLM by nodeLLMRequestMultiple(name = "planner-llm")
            //执行工具
            val executeTools by nodeExecuteMultipleTools()
            //把工具结果喂给llm
            val sendToolResults by nodeLLMSendMultipleToolResults()

            // 输出LLM生成的消息到UI 回调 onAssistantMessage
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }

            edge(nodeStart forwardTo nodeRequestLLM)


            //返回的是联合类型
            edge( //nodeRequestLLM必须调用接口返回tool不然 executeTools就无法正常入参导致报错
                nodeRequestLLM forwardTo executeTools
                        onMultipleToolCalls { true }
            )

            edge( //适配不输出tool直接返回的情况
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            edge(executeTools forwardTo sendToolResults)
            edge(
                sendToolResults forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            edge(nodeAssistantMessage forwardTo nodeFinish)
        }
    }

}


//{"messages":[{"role":"system","content":"I'm a professional chef skilled in various cooking techniques. If the user's inquiry isn't related to cooking, I'll still provide a brief response. All replies will be clear and concise.\n Your goal is to recommend recipes from the local knowledge base.\n - When the user asks for something to eat, use tools to search recipes."},{"role":"user","content":"水煮鱼怎么做"},{"role":"assistant","tool_calls":[{"id":"019c2cc59623fa5e946b63eea33155dc","function":{"name":"RecipeSearchTool","arguments":"{\"query\": \"水煮鱼\"}"},"type":"function"}]},{"role":"tool","content":"{\"contextString\":\"The following recipes are retrieved from the knowledge base:\\n[0] Similarity:0.59\\nSource：/Users/jasonmac/Library/Application Support/AI_COOK/embed/cook/documents/c34ed5aa-cbd4-46b8-857a-5fd8e119b4ec\\n# 水煮鱼的做法\\n\\n水煮鱼是一道做法中等难度的硬菜。巴沙鱼富含优质蛋白且脂肪含量低，配合各种时令蔬菜十分营养健康。初学者一般需要 2 小时即可完成。\\n\\n预估烹饪难度：★★★★\\n\\n## 必备原料和工具\\n\\n- 巴沙鱼\\n- 蔬菜（比如土豆片/豆芽/花菜/生菜/……）\\n- 红油豆瓣酱\\n- 藤椒油\\n- 菜籽油\\n- 白胡椒粉\\n- 蒜瓣\\n- 盐\\n- 糖\\n- 量杯\\n- 厨房秤（可选）\\n- 大不锈钢碗\\n\\n## 计算\\n\\n以下用量适合 3 至 5 人食用。\\n\\n- 巴沙鱼 500g\\n- 蔬菜（比如土豆片/豆芽/花菜/生菜/……） 可有不同搭配，推荐合计重量 300g 至 500g\\n- 红油豆瓣酱 40g （不怕辣想多加红油就多加 10 至 20g）\\n- 豆豉 10g （可选）\\n- 藤椒油 10ml\\n- 菜籽油 25ml\\n- 白胡 椒粉 3g\\n- 大蒜 2 瓣\\n- 盐 5g\\n- 糖 2g\\n\\n## 操作\\n\\n- 准备：巴沙鱼若是从冷冻柜里取出，需要放室温自然解冻 5 小时再做切片处理。\\n- 切片：巴沙鱼撇成薄片，约 5cm 长，3cm 宽。\\n- [腌制](../../tips/learn/学习腌.md)：将切好片的巴沙鱼放入大不锈钢碗中\\n- 加入 30g 豆瓣酱，3g 盐，10ml 藤椒油，3g 白胡椒粉\\n- 用手抓匀后加入 5ml 菜籽油收尾封住口味\\n- 常温静置至少 30 分钟入味。\\n- 备菜：大蒜切成蒜末。以 300g 花菜，200g 生菜为例，将花菜与生菜洗净。\\n- 焯水与炒菜：花菜[开水锅焯水](../../tips/learn/学习焯水.md)备用；将生菜洗净晾干，炒熟备用（不用放油）。\\n- 炒豆瓣酱：热锅冷油（菜籽油 20ml），加入 10g 豆瓣酱，10g 豆豉（可选），加入蒜末，**中火**慢炒。\\n- 汆鱼片：加入 150ml 热水，水很快开后加入腌制好的鱼片，轻轻翻动让鱼片在水中散开，加入 2g 盐和 2g 糖调味（此时可根据个人口味调整盐的用量）。水再次沸腾后即可 盛盘。\\n- 盛盘：先将熟的蔬菜盛至大碗中，然后将热的鱼片盛在蔬菜上面，浇上锅中剩余热汤即可！\\n\\n## 附加内容\\n\\n- 垫底的蔬菜组合及用量可自由发挥，但需要注意各种 蔬菜的特点，比如改成土豆的时候，需要将土豆片/土豆块煮熟（可以用筷子戳一戳确认）。\\n- 红油豆瓣酱（辣度）以及盐的用量可根据个人口味调整。\\n- 注意切鱼片的时候可以垂直于鱼片长条的方向先剁成 5cm 的鱼块，然后翻转 90 度斜着撇成薄片。\\n- [腌制](../../tips/learn/学习腌.md)的时候注意不要太用力抓。\\n- [How to Fillet a Fish 怎样给鱼剔骨](https://www.youtube.com/watch?v=uXSgGtMkgro)\\n\\n### 参考资料\\n\\n如果您遵循本指南的制作流程而发现有问题或可以改进的流程，请提出 Issue 或 Pull request 。\\n\\n\\n\"}","tool_call_id":"019c2cc59623fa5e946b63eea33155dc"}],"model":"Qwen/Qwen2.5-7B-Instruct","stream":false,"tools":[{"function":{"name":"RecipeSearchTool","description":"Retrieve semantically relevant cooking recipes from a recipe knowledge base.\n\nThis tool supports natural language recipe discovery based on ingredients,\ncuisines, cooking methods, dietary preferences, and meal scenarios.\nIt performs semantic similarity search rather than keyword matching.\n\nUse this tool when the user is looking for recipe ideas or specific dishes.\nThe retrieved content should be used as contextual reference material,\nnot returned directly to the user.","parameters":{"type":"object","properties":{"query":{"description":"A natural language description of the recipe or dish the user is looking for. The query may include ingredients, cooking methods, cuisines, dietary constraints, or meal context. The input does not need to be precise or structured.","type":"string"}},"required":["query"]}},"type":"function"}]}