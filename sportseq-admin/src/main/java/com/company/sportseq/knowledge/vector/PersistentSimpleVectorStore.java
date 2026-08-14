package com.company.sportseq.knowledge.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 开发兜底向量库：委托 Spring AI {@link SimpleVectorStore}（内存 + 余弦相似度），
 * 并在写入/删除后持久化到本地 JSON，重启时自动加载。
 *
 * <p>仅用于「本机没有 Docker/Redis Stack」的开发场景；生产环境请使用 Redis Stack
 * （{@code spring.ai.rag.vector.type=redis}，默认推荐方案）。</p>
 */
@Slf4j
public class PersistentSimpleVectorStore implements VectorStore {

    private final SimpleVectorStore delegate;

    private final Path persistPath;

    public PersistentSimpleVectorStore(EmbeddingModel embeddingModel, Path persistPath) {
        this.delegate = SimpleVectorStore.builder(embeddingModel).build();
        this.persistPath = persistPath.toAbsolutePath().normalize();
        if (Files.exists(this.persistPath)) {
            try {
                this.delegate.load(this.persistPath.toFile());
                log.info("已从 {} 加载开发用向量数据", this.persistPath);
            } catch (Exception e) {
                log.warn("开发用向量数据加载失败，将从空向量库开始：{}", e.getMessage());
            }
        }
    }

    @Override
    public void add(List<Document> documents) {
        delegate.add(documents);
        persist();
    }

    @Override
    public void delete(List<String> idList) {
        delegate.delete(idList);
        persist();
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        delegate.delete(filterExpression);
        persist();
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return delegate.similaritySearch(request);
    }

    private void persist() {
        try {
            Files.createDirectories(persistPath.getParent());
            delegate.save(persistPath.toFile());
        } catch (IOException e) {
            log.warn("开发用向量数据持久化失败（重启后需重建向量）：{}", e.getMessage());
        }
    }
}
