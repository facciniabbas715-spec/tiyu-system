package com.company.sportseq.ai.service.impl;

import com.company.sportseq.ai.dto.AiChatDTO;
import com.company.sportseq.ai.exception.AiNotConfiguredException;
import com.company.sportseq.ai.exception.AiServiceException;
import com.company.sportseq.ai.service.AiChatService;
import com.company.sportseq.ai.vo.AiChatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 第一阶段：无状态单轮对话，直接使用 Spring AI 标准 ChatClient 接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;

    @Override
    public AiChatVO chat(AiChatDTO dto) {
        try {
            String content = chatClient.prompt()
                    .user(dto.getMessage())
                    .call()
                    .content();
            return new AiChatVO(content == null ? "" : content);
        } catch (AiNotConfiguredException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 客服调用失败", e);
            throw new AiServiceException();
        }
    }
}
