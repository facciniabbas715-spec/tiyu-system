package com.company.sportseq.knowledge.embedding;

import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * Embedding 封装：统一调用 OpenAI 兼容 EmbeddingModel，并把上游失败翻译为 3204。
 *
 * <p>DeepSeek 不提供 Embedding 接口，若 base-url 指向 DeepSeek 则给出明确迁移提示，
 * 避免用户面对含义不明的 404/401。</p>
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    private final RagProperties properties;

    /**
     * 计算单条文本的向量。
     */
    public float[] embed(String text) {
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            throw translate(e);
        }
    }

    /**
     * 批量计算文本向量（保持输入顺序）。
     */
    public List<float[]> embedAll(List<String> texts) {
        try {
            return embeddingModel.embed(texts);
        } catch (Exception e) {
            throw translate(e);
        }
    }

    /**
     * 当前 EmbeddingModel 的向量维度（用于建索引校验）。
     */
    public int dimensions() {
        return embeddingModel.dimensions();
    }

    private KnowledgeException translate(Exception cause) {
        String baseUrl = properties.getEmbedding().getBaseUrl();
        String message = "知识文档向量化（Embedding）失败，请检查 AI_EMBEDDING_API_KEY / AI_EMBEDDING_BASE_URL / AI_EMBEDDING_MODEL 配置";
        if (StringUtils.hasText(baseUrl)
                && baseUrl.toLowerCase(Locale.ROOT).contains("deepseek")) {
            message = "Embedding 调用失败：DeepSeek 不提供 Embedding 接口。请配置支持 Embedding 的 OpenAI 兼容服务，"
                    + "例如 AI_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1 "
                    + "AI_EMBEDDING_MODEL=text-embedding-v3 AI_EMBEDDING_DIMENSIONS=1024";
        }
        return new KnowledgeException(KnowledgeException.EMBEDDING_FAILED, message + "（" + cause.getMessage() + "）");
    }
}
