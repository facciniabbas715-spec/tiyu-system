package com.company.sportseq.knowledge.service.impl;

import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.exception.KnowledgeException;
import com.company.sportseq.knowledge.service.RagPromptBuilder;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.vector.VectorStoreService;
import com.company.sportseq.knowledge.vo.RagDebugVO;
import com.company.sportseq.knowledge.vo.RagHitVO;
import com.company.sportseq.knowledge.vo.RagResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG 问答实现：
 *
 * <ol>
 *   <li>无命中：直接返回「知识库中暂无相关信息。」，不调用 LLM，杜绝编造；</li>
 *   <li>有命中：把片段注入严格约束的 Prompt，低温生成答案；</li>
 *   <li>debug 信息由 {@link RagProperties#isDebugEnabled()} 控制，生产不返回。</li>
 * </ol>
 */
@Service
public class RagServiceImpl implements RagService {

    private final VectorStoreService vectorStoreService;

    private final ChatClient ragChatClient;

    private final RagPromptBuilder promptBuilder;

    private final RagProperties properties;

    public RagServiceImpl(VectorStoreService vectorStoreService,
                          @Qualifier("ragChatClient") ChatClient ragChatClient,
                          RagPromptBuilder promptBuilder,
                          RagProperties properties) {
        this.vectorStoreService = vectorStoreService;
        this.ragChatClient = ragChatClient;
        this.promptBuilder = promptBuilder;
        this.properties = properties;
    }

    @Override
    public boolean isReady() {
        return properties.isEnabled()
                && StringUtils.hasText(properties.getEmbedding().getApiKey());
    }

    @Override
    public List<RagHitVO> retrieve(String question) {
        return vectorStoreService.search(question);
    }

    @Override
    public RagResult answer(String question) {
        List<RagHitVO> hits = vectorStoreService.search(question);
        if (hits.isEmpty()) {
            return new RagResult(RagPromptBuilder.NO_KNOWLEDGE_REPLY,
                    buildDebug(question, false, hits, RagPromptBuilder.NO_KNOWLEDGE_REPLY));
        }
        String content = callModel(question, hits);
        return new RagResult(content, buildDebug(question, true, hits, content));
    }

    private String callModel(String question, List<RagHitVO> hits) {
        try {
            String content = ragChatClient.prompt()
                    .user(promptBuilder.buildUserMessage(question, hits))
                    .call()
                    .content();
            return content == null || content.isBlank() ? RagPromptBuilder.NO_KNOWLEDGE_REPLY : content;
        } catch (KnowledgeException e) {
            throw e;
        } catch (Exception e) {
            throw new KnowledgeException(KnowledgeException.LLM_FAILED,
                    "大模型调用失败，请稍后重试（" + e.getMessage() + "）");
        }
    }

    private RagDebugVO buildDebug(String query, boolean knowledgeUsed, List<RagHitVO> hits, String answer) {
        if (!properties.isDebugEnabled()) {
            return null;
        }
        return new RagDebugVO(query, knowledgeUsed, properties.getTopK(),
                properties.getSimilarityThreshold(), hits, answer);
    }
}
