package com.company.sportseq.knowledge.vector;

import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.embedding.EmbeddingService;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.vo.RagHitVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量存储封装：统一 VectorStore 的建索引 / 写入 / 删除 / 相似度检索。
 *
 * <p>向量 key 与 MySQL chunkId 一一对应：{@code {prefix}{chunkId}}，Redis 侧
 * Document.id 使用无前缀的 chunkId 字符串；检索结果从 metadata 还原文档归属。</p>
 */
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    public static final String METADATA_DOCUMENT_ID = "document_id";

    public static final String METADATA_CHUNK_INDEX = "chunk_index";

    public static final String METADATA_TITLE = "title";

    public static final String METADATA_FILE_TYPE = "file_type";

    private final VectorStore vectorStore;

    private final EmbeddingService embeddingService;

    private final RagProperties properties;

    /**
     * 幂等初始化：先验证 Embedding 可用（把 DeepSeek 等配置问题翻译为 3204），
     * 再对实现了 {@link InitializingBean} 的向量库（如 RedisVectorStore）建索引。
     */
    public void ensureSchema() {
        if (vectorStore instanceof InitializingBean initializing) {
            try {
                embeddingService.dimensions();
                initializing.afterPropertiesSet();
            } catch (KnowledgeException e) {
                throw e;
            } catch (Exception e) {
                throw new KnowledgeException(KnowledgeException.VECTOR_UNAVAILABLE,
                        "向量库初始化失败：请确认 Redis Stack（含 RediSearch）已运行且 Embedding 配置正确（"
                                + e.getMessage() + "）");
            }
        }
    }

    /**
     * 写入并向量化文档（Embedding 由底层 VectorStore 按注入的 EmbeddingModel 计算）。
     */
    public void store(List<Document> documents) {
        ensureSchema();
        try {
            vectorStore.add(documents);
        } catch (Exception e) {
            throw new KnowledgeException(KnowledgeException.VECTOR_UNAVAILABLE,
                    "向量写入失败：请确认 Redis Stack 可用、向量维度与 Embedding 模型一致（" + e.getMessage() + "）");
        }
    }

    /**
     * 按 chunkId 删除向量。
     */
    public void delete(List<Long> chunkIds) {
        try {
            vectorStore.delete(chunkIds.stream().map(String::valueOf).toList());
        } catch (Exception e) {
            throw new KnowledgeException(KnowledgeException.VECTOR_UNAVAILABLE,
                    "向量删除失败：" + e.getMessage());
        }
    }

    /**
     * 相似度检索：按配置的 topK 与阈值返回命中片段（含相似度分）。
     */
    public List<RagHitVO> search(String query) {
        try {
            return vectorStore.similaritySearch(SearchRequest.builder()
                            .query(query)
                            .topK(properties.getTopK())
                            .similarityThreshold(properties.getSimilarityThreshold())
                            .build())
                    .stream()
                    .map(VectorStoreService::toHit)
                    .toList();
        } catch (Exception e) {
            throw new KnowledgeException(KnowledgeException.VECTOR_UNAVAILABLE,
                    "向量检索失败：请确认知识库已完成向量构建且 Redis Stack 可用（" + e.getMessage() + "）");
        }
    }

    private static RagHitVO toHit(Document document) {
        return new RagHitVO(
                parseLong(document.getMetadata().get(METADATA_DOCUMENT_ID)),
                stringValue(document.getMetadata().get(METADATA_TITLE)),
                parseInt(document.getMetadata().get(METADATA_CHUNK_INDEX)),
                document.getText(),
                document.getScore());
    }

    private static Long parseLong(Object value) {
        return value == null ? null : Long.parseLong(String.valueOf(value));
    }

    private static Integer parseInt(Object value) {
        return value == null ? null : Integer.parseInt(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
