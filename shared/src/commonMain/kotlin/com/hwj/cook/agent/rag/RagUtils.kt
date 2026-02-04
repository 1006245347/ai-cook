package com.hwj.cook.agent.rag

import ai.koog.embeddings.base.Vector
import ai.koog.rag.base.DocumentStorageWithPayload
import ai.koog.rag.base.DocumentWithPayload
import ai.koog.rag.base.RankedDocument
import ai.koog.rag.base.files.FileMetadata
import ai.koog.rag.vector.DocumentEmbedder
import ai.koog.rag.vector.VectorStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * @author by jason-何伟杰，2026/2/4
 * des:其实绑定了向量也够用，所有的信息都存在document上， storage.store(document, vector)
 * The DocumentStorage interface that defines core operations for storing, reading, and deleting documents
 * DocumentStorage 接口，该接口定义了存储、读取和删除文档的核心操作
 * The DocumentStorageWithPayload interface that extends document storage with support for associated metadata or payload
 * 扩展文档存储支持相关元数据或有效负载的 DocumentStorageWithPayload 接口
 * The RankedDocumentStorage interface that extends document storage with ranking capabilities based on query relevance
 * 基于查询相关性的排名功能的 RankedDocumentStorage 接口，扩展文档存储
 * The TextDocumentReader interface for transforming documents into text representation
 * 将文档转换为文本表示的 TextDocumentReader 接口
 * Support for generic document types, allowing flexibility in the types of documents that can be stored and retrieved
 * 支持通用文档类型，允许存储和检索的文档类型具有灵活性
 */
public interface ChunkDocumentStorage<Document> : DocumentStorageWithPayload<Document, String> {
    override suspend fun readWithPayload(documentId: String): DocumentWithPayload<Document, String>? {
        TODO("Not yet implemented")
    }

    override suspend fun getPayload(documentId: String): String? {
        TODO("Not yet implemented")
    }

    override fun allDocumentsWithPayload(): Flow<DocumentWithPayload<Document, String>> {
        TODO("Not yet implemented")
    }

    override suspend fun store(document: Document, data: String): String {
        TODO("Not yet implemented")
    }
}

public interface RankedChunkDocumentStorage<Document> : ChunkDocumentStorage<Document> {
    public fun rankDocuments(query: String): Flow<RankedDocument<Document>>
}

//public interface ChunkVectorStorage<Document>: DocumentStorageWithPayload<Document, Vector>
public open class ChunkEmbeddingBaseDocumentStorage<Document>(
    private val embedder: DocumentEmbedder<Document>,
    private val storage: VectorStorage<Document>,
) : RankedChunkDocumentStorage<Document> {


    override fun rankDocuments(query: String): Flow<RankedDocument<Document>> = flow {
        val queryVector = embedder.embed(query)
        storage.allDocumentsWithPayload().collect { (document, documentVector) ->
            emit(
                RankedDocument(
                    document = document,
                    similarity = 1.0 - embedder.diff(queryVector, documentVector)
                )
            )
        }
    }

    override suspend fun store(document: Document, data: String): String {
        val vector = embedder.embed(document)
        //data

        return storage.store(document, vector)
    }

    override suspend fun delete(documentId: String): Boolean {
        return storage.delete(documentId)
    }

    override suspend fun read(documentId: String): Document? {
        return storage.readWithPayload(documentId)?.let { (document, _) -> document }
    }

    override fun allDocuments(): Flow<Document> = flow {
        storage.allDocumentsWithPayload().collect {
            emit(it.document)
        }
    }
}