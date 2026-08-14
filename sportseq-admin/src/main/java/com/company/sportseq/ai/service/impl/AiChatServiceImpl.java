package com.company.sportseq.ai.service.impl;

import com.company.sportseq.ai.dto.AiChatDTO;
import com.company.sportseq.ai.exception.AiNotConfiguredException;
import com.company.sportseq.ai.exception.AiServiceException;
import com.company.sportseq.ai.service.AiChatService;
import com.company.sportseq.ai.vo.AiChatVO;
import com.company.sportseq.knowledge.service.RagService;
import com.company.sportseq.knowledge.vo.RagResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI 客服编排：RAG 就绪时走知识库检索回答，否则回退到无状态纯 LLM 对话。
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;

    private final RagService ragService;

    public AiChatServiceImpl(@Qualifier("aiChatClient") ChatClient chatClient, RagService ragService) {
        this.chatClient = chatClient;
        this.ragService = ragService;
    }

    @Override
    public AiChatVO chat(AiChatDTO dto) {
        if (ragService.isReady()) {
            try {
                RagResult result = ragService.answer(dto.getMessage());
                return new AiChatVO(result.content(), result.debug());
            } catch (AiNotConfiguredException e) {
                throw e;
            } catch (Exception e) {
                log.error("RAG 客服调用失败", e);
                throw new AiServiceException();
            }
        }
        try {
            String content = chatClient.prompt()
                    .user(dto.getMessage())
                    .call()
                    .content();
            return new AiChatVO(content == null ? "" : content, null);
        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 客服调用失败", e);
            throw new AiServiceException();
        }
    }
}
