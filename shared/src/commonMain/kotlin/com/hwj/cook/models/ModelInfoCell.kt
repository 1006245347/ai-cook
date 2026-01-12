package com.hwj.cook.models

import kotlinx.serialization.Serializable

/**
 * @author by jason-何伟杰，2025/10/15
 * des:大模型接口参数
 */
@Serializable
data class ModelInfoCell(
    var apiKey: String,     //访问token
    var modelName: String,
    var baseUrl: String,    //大模型域名
    var chatCompletionPath: String,     //对话接口地址
    var embeddingsPath: String?,         //具备向量化的接口地址
    var alias: String?,     //模型别名或平台
    var default: Boolean = false //默认模型
)