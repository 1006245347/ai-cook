package com.hwj.cook.models

import ai.koog.rag.base.DocumentWithPayload
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable

//一条RAG证据
@Serializable
data class RagEvidence(
    val document: String,
    val payload: RagPayload?,
    val similarity: Double
)

@Serializable
data class RagPayload(
    val documentId: String,
    val chunkIndex:Int,
    val sourcePath: String?=null
)

//Tools返回结果
data class RagResult(val query: String, val evidence: List<RagEvidence>)