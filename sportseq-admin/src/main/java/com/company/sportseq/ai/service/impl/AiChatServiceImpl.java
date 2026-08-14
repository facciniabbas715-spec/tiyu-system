package com.company.sportseq.ai.service.impl;

import com.company.sportseq.ai.dto.AiChatDTO;
import com.company.sportseq.ai.exception.AiNotConfiguredException;
import com.company.sportseq.ai.exception.AiServiceException;
import com.company.sportseq.ai.service.AiChatPrompt;
import com.company.sportseq.ai.service.AiChatService;
import com.company.sportseq.ai.tool.AiToolCatalog;
import com.company.sportseq.ai.tool.AiToolTrace;
import com.company.sportseq.ai.vo.AiChatVO;
import com.company.sportseq.ai.vo.AiDebugVO;
import com.company.sportseq.ai.vo.AiToolCallVO;
import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.vo.RagDebugVO;
import com.company.sportseq.knowledge.vo.RagResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 客服编排：工具就绪时走 Tool Calling（知识题→知识工具，业务题→业务工具），
 * 否则回退到 RAG 检索回答 / 无状态纯 LLM 对话。
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;

    private final RagService ragService;

    private final AiToolCatalog toolCatalog;

    private final RagProperties ragProperties;

    public AiChatServiceImpl(@Qualifier("aiChatClient") ChatClient chatClient,
                             RagService ragService,
                             AiToolCatalog toolCatalog,
                             RagProperties ragProperties) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.toolCatalog = toolCatalog;
        this.ragProperties = ragProperties;
    }

    @Override
    public AiChatVO chat(AiChatDTO dto) {
        String question = dto.getMessage();
        boolean debugEnabled = ragProperties.isDebugEnabled();
        AiToolTrace.begin(debugEnabled);
        try {
            if (!toolCatalog.callbacks().isEmpty()) {
                String content = callWithTools(question);
                return new AiChatVO(content, toolDebug(question, content));
            }
            if (ragService.isReady()) {
                RagResult result = ragService.answer(question);
                return new AiChatVO(result.content(), ragDebug(question, result));
            }
            String content = chatClient.prompt()
                    .user(question)
                    .call()
                    .content();
            String answer = content == null ? "" : content;
            return new AiChatVO(answer, debugEnabled
                    ? new AiDebugVO(question, "CHAT", List.of(), false, List.of(), answer)
                    : null);
        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 客服调用失败", e);
            throw new AiServiceException();
        } finally {
            AiToolTrace.end();
        }
    }

    private String callWithTools(String question) {
        String content = chatClient.prompt()
                .system(AiChatPrompt.TOOL_SYSTEM_PROMPT)
                .user(question)
                .toolCallbacks(toolCatalog.callbacks())
                .call()
                .content();
        return content == null || content.isBlank() ? "" : content;
    }

    private AiDebugVO toolDebug(String question, String answer) {
        if (!ragProperties.isDebugEnabled()) {
            return null;
        }
        List<AiToolCallVO> calls = AiToolTrace.currentCalls();
        return new AiDebugVO(question, intentOf(calls), calls,
                AiToolTrace.knowledgeUsed(), AiToolTrace.currentKnowledgeHits(), answer);
    }

    private AiDebugVO ragDebug(String question, RagResult result) {
        RagDebugVO debug = result.debug();
        if (debug == null) {
            return null;
        }
        return new AiDebugVO(question, "KNOWLEDGE", List.of(),
                debug.knowledgeUsed(), debug.hits(), debug.answer());
    }

    private String intentOf(List<AiToolCallVO> calls) {
        boolean knowledge = calls.stream().anyMatch(call -> "searchKnowledge".equals(call.name()));
        boolean business = calls.stream().anyMatch(call -> !"searchKnowledge".equals(call.name()));
        if (knowledge && business) {
            return "MIXED";
        }
        if (knowledge) {
            return "KNOWLEDGE";
        }
        if (business) {
            return "BUSINESS";
        }
        return "CHAT";
    }
}
