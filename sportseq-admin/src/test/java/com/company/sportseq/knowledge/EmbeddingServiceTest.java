package com.company.sportseq.knowledge;

import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.embedding.EmbeddingService;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Embedding 服务单元测试：转发向量计算并把上游异常翻译为 3204。
 */
class EmbeddingServiceTest {

    @Test
    void embed_shouldReturnVectorFromModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(anyString())).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });
        EmbeddingService service = new EmbeddingService(model, properties());

        float[] vector = service.embed("篮球保养");

        assertArrayEquals(new float[] { 0.1f, 0.2f, 0.3f }, vector);
    }

    @Test
    void embedAll_shouldDelegateToModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(List.of("a", "b"))).thenReturn(List.of(new float[] { 1f }, new float[] { 2f }));
        EmbeddingService service = new EmbeddingService(model, properties());

        List<float[]> vectors = service.embedAll(List.of("a", "b"));

        assertEquals(2, vectors.size());
        assertArrayEquals(new float[] { 2f }, vectors.get(1));
    }

    @Test
    void embed_shouldWrapUpstreamFailureAsKnowledgeException() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(anyString())).thenThrow(new IllegalStateException("401 unauthorized"));
        EmbeddingService service = new EmbeddingService(model, properties());

        KnowledgeException exception = assertThrows(KnowledgeException.class,
                () -> service.embed("篮球保养"));

        assertEquals(KnowledgeException.EMBEDDING_FAILED, exception.getCode());
        assertTrue(exception.getMessage().contains("Embedding"));
    }

    @Test
    void embed_shouldHintAlternativeProviderWhenDeepSeekConfigured() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.embed(anyString())).thenThrow(new IllegalStateException("404 not found"));
        RagProperties props = properties();
        props.getEmbedding().setBaseUrl("https://api.deepseek.com");
        EmbeddingService service = new EmbeddingService(model, props);

        KnowledgeException exception = assertThrows(KnowledgeException.class,
                () -> service.embed("篮球保养"));

        assertTrue(exception.getMessage().contains("DeepSeek"),
                "DeepSeek 不提供 Embedding 接口，应给出切换服务商的提示");
    }

    private RagProperties properties() {
        RagProperties props = new RagProperties();
        props.getEmbedding().setBaseUrl("https://api.openai.com");
        props.getEmbedding().setModel("text-embedding-3-small");
        return props;
    }
}
