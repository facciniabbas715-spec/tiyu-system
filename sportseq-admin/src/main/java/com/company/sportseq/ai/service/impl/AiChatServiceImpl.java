package com.company.sportseq.ai.service.impl;

import com.company.sportseq.ai.dto.AiChatDTO;
import com.company.sportseq.ai.exception.AiNotConfiguredException;
import com.company.sportseq.ai.exception.AiRateLimitedException;
import com.company.sportseq.ai.exception.AiServiceException;
import com.company.sportseq.ai.exception.AiToolCallLimitException;
import com.company.sportseq.ai.service.AiChatPrompt;
import com.company.sportseq.ai.service.AiChatHistoryService;
import com.company.sportseq.ai.service.AiChatService;
import com.company.sportseq.ai.service.AiRateLimiter;
import com.company.sportseq.ai.tool.AiToolCatalog;
import com.company.sportseq.ai.tool.AiToolTrace;
import com.company.sportseq.ai.vo.AiChatVO;
import com.company.sportseq.ai.vo.AiDebugVO;
import com.company.sportseq.ai.vo.AiHistoryMessage;
import com.company.sportseq.ai.vo.AiToolCallVO;
import com.company.sportseq.knowledge.config.RagProperties;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.vo.RagDebugVO;
import com.company.sportseq.knowledge.vo.RagResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
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

    private final AiRateLimiter rateLimiter;

    private final AiChatHistoryService historyService;

    private final Environment environment;

    public AiChatServiceImpl(@Qualifier("aiChatClient") ChatClient chatClient,
                             RagService ragService,
                             AiToolCatalog toolCatalog,
                             RagProperties ragProperties,
                             AiRateLimiter rateLimiter,
                             AiChatHistoryService historyService,
                             Environment environment) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.toolCatalog = toolCatalog;
        this.ragProperties = ragProperties;
        this.rateLimiter = rateLimiter;
        this.historyService = historyService;
        this.environment = environment;
    }

    @Override
    public AiChatVO chat(AiChatDTO dto) {
        rateLimiter.check();
        String question = dto.getMessage();
        boolean debugEnabled = ragProperties.isDebugEnabled() && !isProdProfile();
        AiToolTrace.begin(debugEnabled);
        try {
            if (!toolCatalog.callbacks().isEmpty()) {
                String content = callWithTools(question);
                historyService.append(question, content);
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
        } catch (AiRateLimitedException | AiToolCallLimitException | AiNotConfiguredException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("AI 上游响应异常", e);
            if (e.getRawStatusCode() == 429) {
                throw new AiServiceException(3103, "AI 服务繁忙（触发上游限流），请稍后再试");
            }
            throw new AiServiceException();
        } catch (ResourceAccessException e) {
            log.error("AI 上游连接异常", e);
            throw new AiServiceException(3104, "AI 服务连接超时，请稍后重试");
        } catch (Exception e) {
            log.error("AI 客服调用失败", e);
            throw new AiServiceException();
        } finally {
            AiToolTrace.end();
        }
    }

    private String callWithTools(String question) {
        List<Message> history = historyService.load().stream()
                .map(AiChatServiceImpl::toMessage)
                .toList();
        String content = chatClient.prompt()
                .system(AiChatPrompt.TOOL_SYSTEM_PROMPT)
                .messages(history)
                .user(question)
                .toolCallbacks(toolCatalog.callbacks())
                .call()
                .content();
        return content == null || content.isBlank() ? "" : content;
    }

    private static Message toMessage(AiHistoryMessage historyMessage) {
        return "user".equals(historyMessage.role())
                ? new UserMessage(historyMessage.content())
                : new AssistantMessage(historyMessage.content());
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
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
