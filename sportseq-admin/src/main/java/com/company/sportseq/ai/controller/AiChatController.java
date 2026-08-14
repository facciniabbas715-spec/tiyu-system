package com.company.sportseq.ai.controller;

import com.company.sportseq.ai.dto.AiChatDTO;
import com.company.sportseq.ai.service.AiChatService;
import com.company.sportseq.ai.vo.AiChatVO;
import com.company.sportseq.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能客服接口：POST /api/ai/chat。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:chat')")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiChatService.chat(dto));
    }
}
