package com.company.sportseq.knowledge;

import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.embedding.EmbeddingService;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.vector.VectorStoreService;
import com.company.sportseq.knowledge.vo.RagHitVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * 向量存储服务单元测试：稳定 id/元数据、删除映射、检索参数与异常翻译。
 */
class VectorStoreServiceTest {

    private static final Document DOC_1 = Document.builder()
            .id("11")
            .text("篮球使用后应清洁表面灰尘。")
            .metadata(Map.of(
                    VectorStoreService.METADATA_DOCUMENT_ID, 5L,
                    VectorStoreService.METADATA_CHUNK_INDEX, 0,
                    VectorStoreService.METADATA_TITLE, "篮球知识",
                    VectorStoreService.METADATA_FILE_TYPE, "md"))
            .build();

    @Test
    void store_shouldAddDocumentsWithStableIdsAndMetadata() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService(), properties());

        service.store(List.of(DOC_1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document stored = captor.getValue().get(0);
        assertEquals("11", stored.getId());
        assertEquals(5L, stored.getMetadata().get(VectorStoreService.METADATA_DOCUMENT_ID));
        assertEquals("篮球知识", stored.getMetadata().get(VectorStoreService.METADATA_TITLE));
    }

    @Test
    void delete_shouldDelegateWithChunkIdsAsDocumentIds() {
        VectorStore vectorStore = mock(VectorStore.class);
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService(), properties());

        service.delete(List.of(11L, 12L));

        verify(vectorStore).delete(List.of("11", "12"));
    }

    @Test
    void search_shouldApplyTopKAndThresholdAndParseHits() {
        VectorStore vectorStore = mock(VectorStore.class);
        Document hit = Document.builder()
                .id("11")
                .text("篮球使用后应清洁表面灰尘。")
                .metadata(Map.of(
                        VectorStoreService.METADATA_DOCUMENT_ID, "5",
                        VectorStoreService.METADATA_CHUNK_INDEX, "0",
                        VectorStoreService.METADATA_TITLE, "篮球知识",
                        VectorStoreService.METADATA_FILE_TYPE, "md"))
                .score(0.83)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit));
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService(), properties());

        List<RagHitVO> hits = service.search("篮球应该怎么保养？");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertEquals(4, captor.getValue().getTopK());
        assertEquals(0.7, captor.getValue().getSimilarityThreshold());
        assertEquals("篮球应该怎么保养？", captor.getValue().getQuery());
        assertEquals(1, hits.size());
        assertEquals(5L, hits.get(0).documentId());
        assertEquals("篮球知识", hits.get(0).title());
        assertEquals(0.83, hits.get(0).similarity());
    }

    @Test
    void ensureSchema_shouldSkipNonInitializingStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        EmbeddingService embeddingService = embeddingService();
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService, properties());

        service.ensureSchema();

        verify(embeddingService, never()).dimensions();
    }

    @Test
    void ensureSchema_shouldValidateEmbeddingThenInitializeInitializingStore() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class, withSettings().extraInterfaces(InitializingBean.class));
        EmbeddingService embeddingService = embeddingService();
        when(embeddingService.dimensions()).thenReturn(1024);
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService, properties());

        service.ensureSchema();

        verify(embeddingService).dimensions();
        verify((InitializingBean) vectorStore).afterPropertiesSet();
    }

    @Test
    void search_shouldWrapFailureAsVectorUnavailable() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("connection refused"));
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService(), properties());

        KnowledgeException exception = assertThrows(KnowledgeException.class,
                () -> service.search("篮球应该怎么保养？"));

        assertEquals(KnowledgeException.VECTOR_UNAVAILABLE, exception.getCode());
    }

    @Test
    void store_shouldWrapFailureAsVectorUnavailable() {
        VectorStore vectorStore = mock(VectorStore.class);
        doThrow(new IllegalStateException("unknown command FT.CREATE"))
                .when(vectorStore).add(any());
        VectorStoreService service = new VectorStoreService(vectorStore, embeddingService(), properties());

        KnowledgeException exception = assertThrows(KnowledgeException.class,
                () -> service.store(List.of(DOC_1)));

        assertEquals(KnowledgeException.VECTOR_UNAVAILABLE, exception.getCode());
    }

    private EmbeddingService embeddingService() {
        return mock(EmbeddingService.class);
    }

    private RagProperties properties() {
        RagProperties props = new RagProperties();
        props.setTopK(4);
        props.setSimilarityThreshold(0.7);
        props.getVector().setPrefix("sportseq:knowledge:chunk:");
        props.getEmbedding().setBaseUrl("https://api.openai.com");
        return props;
    }
}
