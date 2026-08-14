package com.company.sportseq.knowledge;

import com.company.sportseq.knowledge.vector.PersistentSimpleVectorStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 开发兜底向量库：写入后持久化到文件，重启后能重新加载并检索（生产使用 Redis Stack，此处仅为本地开发兜底）。
 */
class PersistentSimpleVectorStoreTest {

    @Test
    void store_shouldPersistAndReloadDocuments(@TempDir Path tempDir) throws IOException {
        EmbeddingModel model = stubEmbeddingModel();
        Path persistFile = tempDir.resolve("kb/vectors.json");
        Document document = Document.builder()
                .id("1")
                .text("篮球保养要点：清洁表面并存放于干燥阴凉处")
                .metadata(Map.of("title", "篮球知识"))
                .build();

        PersistentSimpleVectorStore store = new PersistentSimpleVectorStore(model, persistFile);
        store.add(List.of(document));

        assertTrue(Files.exists(persistFile), "写入后应持久化到文件");

        PersistentSimpleVectorStore reloaded = new PersistentSimpleVectorStore(model, persistFile);
        List<Document> results = reloaded.similaritySearch(SearchRequest.builder()
                .query("篮球保养")
                .topK(2)
                .similarityThreshold(0.5)
                .build());

        assertEquals(1, results.size());
        assertEquals("1", results.get(0).getId());
        assertEquals("篮球知识", results.get(0).getMetadata().get("title"));
    }

    @Test
    void delete_shouldRemoveAndPersist(@TempDir Path tempDir) {
        EmbeddingModel model = stubEmbeddingModel();
        Path persistFile = tempDir.resolve("vectors.json");
        PersistentSimpleVectorStore store = new PersistentSimpleVectorStore(model, persistFile);
        store.add(List.of(Document.builder().id("1").text("内容").build()));

        store.delete(List.of("1"));

        PersistentSimpleVectorStore reloaded = new PersistentSimpleVectorStore(model, persistFile);
        assertTrue(reloaded.similaritySearch(SearchRequest.builder()
                .query("内容")
                .topK(2)
                .similarityThreshold(0.5)
                .build()).isEmpty());
    }

    private EmbeddingModel stubEmbeddingModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(any(Document.class))).thenReturn(new float[] { 1f, 0f });
        when(model.embed(anyString())).thenReturn(new float[] { 1f, 0f });
        return model;
    }
}
